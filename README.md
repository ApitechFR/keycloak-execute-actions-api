# Keycloak Execute Actions REST API

This custom Keycloak REST API provides an extra endpoint to generate (and get) an `execute-actions` link.

It works exactly the same as `execute-actions-email` from Keycloak source code, but returns the link (_instead of an empty response_) and avoids sending the email (_leaving you the responsibility to send this link to the user_).

It adds an endpoint `PUT ${serverDomain}/realms/${realm}/execute-actions` that expects the following parameters : `userId`, `clientId`, `redirectUri` and `actions`.

## Keycloak Support

Designed for Keycloak `>= 21.x.x`. May work with other versions, not tested yet.

## Deployment

### Standalone install

* Download `dist/keycloak-execute-actions-21.x.x.jar` from this repository
* Add it to `$KEYCLOAK_HOME/standalone/deployments/`

### Docker install

If you are using the official Docker image, here is a `Dockerfile` that automate the installation procedure described above:
```
FROM quay.io/keycloak/keycloak:21.0.0

COPY keycloak-execute-actions-21.x.x.jar /opt/keycloak/providers/keycloak-execute-actions.jar
```

## Development

### Build library

```bash
    $ ./gradlew build
```

## Credits

This repository is hugely inspired by [Keycloak Configuration Token REST API](https://github.com/looorent/keycloak-configurable-token-api). Thanks to [its owner](https://github.com/looorent) for creating and sharing a codebase with a Keycloak provider that provides a new endpoint **and** works well with Keycloak `21.x.x` !
