# GET /profile/:username

Retrieve the active cape URL for a registered user.

## Request

**Method:** `GET`  
**URL:** `/profile/{username}`

### URL Parameters

| Parameter | Type   | Required | Description                   |
|-----------|--------|----------|-------------------------------|
| `username` | string | yes      | Minecraft username (IGN) to look up |

## Response

### Success (200)

If the user is registered and has a cape equipped:

```json
{
    "cape": "http://localhost:8000/assets/capes/1490.png"
}
```

If the user is registered but has no cape equipped (`capeid` is `"null"`):

```json
{
    "cape": "http://localhost:8000/assets/capes/null.png"
}
```

If the user is not registered (no `user_meta/{username}.json` file exists), a fallback response is returned:

```json
{
    "cape": "http://localhost:8000/assets/capes/null.png"
}
```

### Field

| Field  | Type   | Description                                  |
|--------|--------|----------------------------------------------|
| `cape` | string | Full URL to the player's cape PNG texture, or the fallback `null.png` |

### Errors

| Status | Condition                                           |
|--------|-----------------------------------------------------|
| 500    | Filesystem read failure or unexpected server error  |

## Example

```bash
curl http://localhost:8000/profile/Notch
```
