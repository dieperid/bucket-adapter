# API Documentation

This document describes the current object-based API exposed by `BucketController`.

## Base URLs

- `http://localhost:8080/api`
- `http://localhost:8080/api/v1`

Both prefixes expose the same endpoints.

## Endpoints

| Action | HTTP Method | Endpoint | Parameters | Request Content-Type | Response | HTTP Status |
| --- | --- | --- | --- | --- | --- | --- |
| Upload object | POST | `/files` | `remote` (String), `file` (multipart file) | `multipart/form-data` | Empty body | `201 Created` |
| Download object | GET | `/files/download` | `remote` (String) | None | Binary bytes (`application/octet-stream`) | `200 OK` |
| Update object | PUT | `/files` | `remote` (String), `file` (multipart file) | `multipart/form-data` | Empty body | `204 No Content` |
| Delete object/prefix | DELETE | `/files` | `remote` (String), `recursive` (boolean, optional, default `false`) | None | Empty body | `204 No Content` |
| List objects | GET | `/files` | `path` (String) | None | JSON array of strings | `200 OK` |
| Share object | POST | `/files/share` | `remote` (String), `expirationTime` (int, seconds) | None | Signed URL string | `200 OK` |

## cURL Examples

### Upload

```bash
curl -i -X POST "http://localhost:8080/api/files" \
  -F "remote=my-bucket/path/in/bucket/hello.txt" \
  -F "file=@/Users/you/Documents/hello.txt"
```

### Download

```bash
curl -L "http://localhost:8080/api/files/download?remote=my-bucket/path/in/bucket/hello.txt" \
  --output hello.txt
```

### Update

```bash
curl -i -X PUT "http://localhost:8080/api/files" \
  -F "remote=my-bucket/path/in/bucket/hello.txt" \
  -F "file=@/Users/you/Documents/hello-updated.txt"
```

### Delete

```bash
curl -i -X DELETE "http://localhost:8080/api/files?remote=my-bucket/path/in/bucket/hello.txt&recursive=false"
```

### List

```bash
curl -s "http://localhost:8080/api/files?path=my-bucket/path/in/bucket/" | jq
```

### Share

```bash
curl -s -X POST "http://localhost:8080/api/files/share?remote=my-bucket/path/in/bucket/hello.txt&expirationTime=3600"
```

## Notes

1. `remote` and `path` values should use bucket-style paths, e.g. `bucket-name/object/key.txt`.
2. `download` returns raw bytes; use `--output` in `curl` to save to a file.
3. Use `/api/v1` instead of `/api` if you want versioned routes explicitly.
