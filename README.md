# Role-Based Access and Consent Management

A Java 21 project demonstrating role-based and consent-aware access decisions for patient data.

## Features

- Role-based access decisions for clinical, billing, privacy, and administrative users.
- Care-team membership checks with time-limited assignments.
- Patient consent scoped by data category and purpose, including expiry and revocation.
- Restrictions for sensitive categories such as mental health, substance-use treatment, HIV status, and psychotherapy notes.
- Minimum-necessary field masking for billing staff.
- Time-limited proxy authorization for guardians and proxies.
- Emergency break-glass access for eligible clinicians outside the care team.
- One-hour emergency grants and privacy review tasks due within 24 hours.
- Audit events for access decisions and emergency access workflows.
- Session revalidation to check current authorization and consent.

## Requirements

- Java Development Kit (JDK) 21
- Git, if you want to clone or contribute to the repository

Maven is not required to be installed globally; the project includes the Maven Wrapper.

## Run the tests

Open a terminal in the project directory and run:

### Windows PowerShell

```powershell
.\mvnw.cmd clean test
```

### macOS or Linux

```bash
./mvnw clean test
```

The tests use JUnit 5. The current verified test run completed with 68 tests passing and no failures or errors.

## Project structure

```text
role-based-access-consent-management/
├── .mvn/
│   └── wrapper/
├── src/
│   ├── main/java/com/example/accessconsent/
│   └── test/java/com/example/accessconsent/
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Important limitations

This project is an educational access-control prototype, not a certified or production-ready HIPAA compliance solution.

- The audit store is in-memory; events are not persisted across application restarts.
- Production use would require durable audit storage, authenticated identities, secure data integration, and enforcement of authorization decisions at every data-access boundary.
- Legal and organizational policies must be reviewed before using these rules with real patient data.

## Security note

Do not commit credentials, private keys, access tokens, real patient information, or other sensitive data to source control.
