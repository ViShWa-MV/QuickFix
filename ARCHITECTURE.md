# QuickFix — Target Architecture

QuickFix is a two-sided **home-services marketplace**: customers find and book
skilled technicians (AC, washing machine, refrigerator, etc.), and technicians
manage their profile, availability, and jobs.

This document describes the **target architecture** — where the system is
headed. It is a design blueprint, not the current state of the code. See
[Current state](#current-state--migration) at the end for the gap.

---

## Product model (decided)

Three product decisions anchor the whole design:

| Decision | Choice | Why |
| --- | --- | --- |
| **Booking model** | **Pick + accept / fallback** | Customer browses ranked, available technicians and picks one; the technician accepts or declines; on decline/timeout the customer is offered the next best. Keeps "choose your technician" trust while staying availability-accurate. |
| **Availability** | **Slot / calendar** | Technicians publish working hours and bookable slots with capacity. Home visits are scheduled, not instant, and slots prevent double-booking. |
| **Pricing** | **Catalog base price + in-job quote** | A base/visit fee is shown upfront; after inspection the technician can propose a quote (parts/labor) that the customer approves in-app. Transparent and realistic. |

---

## Service map

The SPA serves both the customer and technician experiences and talks to a
single gateway origin. Behind it sit three services, each owning one database.

```mermaid
flowchart TB
  SPA["SPA<br/>customer app + technician app"]
  GW["API Gateway :8080<br/>validate JWT · route · terminate CORS"]

  subgraph US["user-service :8081 — Identity"]
    U1["Google OIDC login + JWT issue"]
    U2["customers · technician accounts · roles"]
  end
  subgraph CS["catalog-service :8082 — Catalog + Providers"]
    C1["categories · services · base prices"]
    C2["technician profiles · skills · service areas"]
    C3["availability slots · search & ranking"]
  end
  subgraph BS["booking-service :8083 — the heart (NEW)"]
    B1["booking lifecycle · matching · fallback"]
    B2["slot locking · quotes"]
    B3["payments* · reviews*"]
  end

  DBU[("quickfix_user")]
  DBC[("quickfix_catalog")]
  DBB[("quickfix_booking")]

  SPA --> GW
  GW --> US
  GW --> CS
  GW --> BS
  US --> DBU
  CS --> DBC
  BS --> DBB
```

`*` Payments and Reviews begin **inside** booking-service and are split out only
if they grow. Three services is the right size for this system — resist adding
more until a clear seam demands it.

### Responsibilities

- **user-service (Identity).** Google OIDC login, JWT issuance, and user
  accounts. Everyone — customer, technician, admin — is a `User` with a role.
- **catalog-service (Catalog + Providers).** The service catalog (categories,
  services, base prices), technician *profiles* (skills, service area,
  verification, computed rating), availability slots, and the **search/ranking**
  that powers discovery.
- **booking-service (the heart).** The booking state machine, matching and
  fallback, slot locking, in-job quotes, and initially payments and reviews.

---

## Domain model

```mermaid
erDiagram
  USER ||--o| TECHNICIAN_PROFILE : "is (if technician)"
  TECHNICIAN_PROFILE ||--o{ AVAILABILITY_SLOT : publishes
  CATEGORY ||--o{ SERVICE : groups
  SERVICE ||--o{ BOOKING : "booked as"
  TECHNICIAN_PROFILE ||--o{ BOOKING : fulfils
  USER ||--o{ BOOKING : "books (customer)"
  BOOKING ||--o| QUOTE : "may have"
  BOOKING ||--o| PAYMENT : "settled by"
  BOOKING ||--o| REVIEW : "rated by"

  USER {
    bigint id PK
    string email UK
    string googleId UK
    string name
    string phone
    enum role "CUSTOMER / TECHNICIAN / ADMIN"
  }
  TECHNICIAN_PROFILE {
    bigint id PK
    bigint userId FK
    json skills "category ids"
    int yearsExperience
    json serviceAreas "pincodes"
    enum verificationStatus "PENDING / APPROVED / REJECTED"
    double ratingAvg "computed"
    int ratingCount "computed"
  }
  AVAILABILITY_SLOT {
    bigint id PK
    bigint technicianProfileId FK
    datetime startAt
    datetime endAt
    enum status "OPEN / HELD / BOOKED"
  }
  CATEGORY {
    bigint id PK
    string name UK
  }
  SERVICE {
    bigint id PK
    bigint categoryId FK
    string name
    double basePrice
    boolean active
  }
  BOOKING {
    bigint id PK
    bigint customerId
    bigint technicianProfileId
    bigint serviceId
    bigint slotId
    enum status
    double baseAmount
    datetime requestedAt
    datetime expiresAt
  }
  QUOTE {
    bigint id PK
    bigint bookingId FK
    json lineItems
    double amount
    enum status "PROPOSED / APPROVED / REJECTED"
  }
  PAYMENT {
    bigint id PK
    bigint bookingId FK
    double amount
    double commission
    double payoutAmount
    enum status
  }
  REVIEW {
    bigint id PK
    bigint bookingId FK
    bigint byUserId
    int rating
    string comment
  }
```

Key modelling rules:

- **Account vs. profile are separate.** A technician is a `User` (in
  user-service) *plus* a `TechnicianProfile` (in catalog-service). This resolves
  today's ambiguity where "technician" exists both as a role and as a standalone
  entity.
- **Rating is computed**, not an editable field — it is derived from `REVIEW`
  rows and cached on the profile as `ratingAvg` / `ratingCount`.
- **Relationships across services are logical ids**, not database foreign keys.
  No cross-service joins.

---

## The technician lifecycle

```
Sign up as TECHNICIAN
  └─ build profile: skills, years of experience, service area (pincodes),
     pricing, photo, KYC documents
  └─ verificationStatus: PENDING ──(admin reviews)──► APPROVED  (now listable)
                                                   └► REJECTED

Set availability: working hours + capacity  ──►  bookable OPEN slots
  (block days off, flip "accepting bookings")

Receive a booking request  ──►  ACCEPT (within N minutes)  or  DECLINE
  accept  → job confirmed, slot locked
  decline → customer offered the next-best technician

Run the job:  CONFIRMED → EN_ROUTE → IN_PROGRESS → COMPLETED
  (optional) propose a Quote mid-job → customer approves in-app

Get paid: platform takes commission → technician payout
Get rated: review feeds back into search ranking
```

Only `APPROVED` technicians appear in search.

---

## Booking lifecycle (state machine)

```mermaid
stateDiagram-v2
  [*] --> REQUESTED : customer picks tech + slot (slot → HELD)
  REQUESTED --> CONFIRMED : technician accepts (slot → BOOKED)
  REQUESTED --> DECLINED : technician declines (slot → OPEN)
  REQUESTED --> EXPIRED : no response in N min (slot → OPEN)
  CONFIRMED --> EN_ROUTE : technician on the way
  EN_ROUTE --> IN_PROGRESS : work started
  IN_PROGRESS --> COMPLETED : work done
  COMPLETED --> CLOSED : paid + reviewed
  CONFIRMED --> CANCELLED : either party, per policy
  EN_ROUTE --> CANCELLED : either party, per policy
  DECLINED --> [*]
  EXPIRED --> [*]
  CANCELLED --> [*]
  CLOSED --> [*]
```

The **slot lock cycle is the critical concurrency rule**: a slot moves
`OPEN → HELD` the moment a booking is requested, `HELD → BOOKED` on accept, and
back to `OPEN` on decline, expiry, or cancellation. This is what lets
pick-and-accept and calendar availability coexist without double-booking.

---

## End-to-end flow

```mermaid
sequenceDiagram
  autonumber
  participant C as Customer (SPA)
  participant CAT as catalog-service
  participant BK as booking-service
  participant T as Technician (SPA)
  C->>CAT: search(category=AC, pincode, window)
  CAT-->>C: ranked techs (APPROVED ∧ skilled ∧ serves area ∧ free slot)
  C->>BK: book(technician, slot, service)
  BK->>BK: create Booking REQUESTED, slot → HELD, set expiresAt
  BK->>T: notify booking request
  alt Technician accepts
    T->>BK: accept
    BK->>BK: CONFIRMED, slot → BOOKED
    T->>BK: EN_ROUTE → IN_PROGRESS
    opt extra work
      T->>BK: propose Quote
      C->>BK: approve Quote
    end
    T->>BK: COMPLETED
    BK->>BK: Payment = base + approved quote, commission, payout
    C->>BK: review (rating + comment)
    BK->>CAT: recompute technician ratingAvg/Count
    BK->>BK: CLOSED
  else Declines / times out
    BK->>BK: DECLINED/EXPIRED, slot → OPEN
    BK-->>C: offer next-best technician
  end
```

---

## Cross-cutting concerns

- **API Gateway** (Spring Cloud Gateway) terminates CORS once, validates the
  JWT once, routes by path prefix, and forwards identity (`X-User-Id` /
  `X-Roles`, or the JWT) to downstream services.
- **AuthZ in each service.** Services trust the forwarded identity and enforce
  *authorization* with `@PreAuthorize` (e.g. only `ADMIN` approves technicians;
  only the owning technician transitions their booking).
- **Validation.** Controllers accept **DTOs with `@Valid`**, never bind request
  bodies straight to entities.
- **Config & secrets.** No secrets in source or git history; signing keys and
  DB credentials come from environment variables or a secret store. Each service
  manages its schema with **Flyway/Liquibase migrations** instead of
  `ddl-auto=update`.
- **Notifications.** Booking events (requested, accepted, completed) drive
  notifications — start with email/DB events, add push later.

---

## Build roadmap

Each step is independently shippable.

1. **Identity & profiles** — technician as a `User` + `TechnicianProfile`
   (skills, service area, verification); admin approval flow.
2. **Availability slots** + **search / ranking** in catalog-service.
3. **booking-service: the state machine** + slot hold / accept / fallback. *(core)*
4. **In-job quote** proposal and approval.
5. **Payments** + commission / payout.
6. **Reviews → computed rating** (closes the loop back to search).
7. **Hardening** — gateway, auth on all services, DTO validation, migrations,
   secret externalization.

---

## Current state & migration

Today's code is an early slice, not this target:

- **user-service** has solid Google OIDC login, a stateless JWT filter, and
  per-user data scoping — the Identity foundation is real.
- **catalog-service** has `Category` / `Service` and a flat `Technician` entity,
  but **no authentication**, no DTO validation, and is **not yet called by the
  SPA**.
- There is **no booking-service** yet — the heart of the product is unbuilt.
- "Technician" is modelled twice (a `Role` value and a catalog entity) with
  nothing linking them; the target splits this into account + profile.
- Dev-only concerns remain: a DB password lives in git history, and both
  services run `ddl-auto=update`.

Getting from here to the target follows the roadmap above, starting with the
technician account/profile split in step 1.
