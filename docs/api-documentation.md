# API Documentation

This file is used to document the API. It describes the available endpoints, how to use them, and provides some tips for getting better feedback and results in the terminal.

## What are the endpoints ?

| Action   | HTTP Method | Endpoint              | Parameters                                               | Description                                   | HTTP Status                                 |
| -------- | ----------- | --------------------- | -------------------------------------------------------- | --------------------------------------------- | ------------------------------------------- |
| Upload   | POST        | `/api/files`          | `localPath` (String), `remoteSrc` (String)               | Upload a local file to the bucket             | 201 Created                                 |
| Download | GET         | `/api/files/download` | `localPath` (String), `remoteSrc` (String)               | Download a file from the bucket to the server | 200 OK                                      |
| Update   | PUT         | `/api/files`          | `localPath` (String), `remoteSrc` (String)               | Replace an existing file in the bucket        | 204 No Content                              |
| Delete   | DELETE      | `/api/files`          | `remoteSrc` (String), `recursive` (boolean, optional)    | Delete a file or folder from the bucket       | 204 No Content                              |
| List     | GET         | `/api/files`          | `path` (String)                                          | List all files in a directory in the bucket   | 200 OK                                      |
| Exists   | GET         | `/api/files/exists`   | `remoteSrc` (String)                                     | Check if a file exists in the bucket          | 200 OK if exists / 404 Not Found if missing |
| Share    | POST        | `/api/files/share`    | `remoteSrc` (String), `expirationTime` (int, in seconds) | Generate a shared link for a file             | 200 OK                                      |

### Notes

1. `remoteSrc`: Logical identifier of the file in the bucket
2. `localPath`: Local path on the server used for upload/download
3. All endpoints are prefixed with `/api/files`
4. `/files` is treated as a REST resource — no verbs in the URL

## How to use them ?

| Action   | cURL Example                                                                                                                   |
| -------- | ------------------------------------------------------------------------------------------------------------------------------ |
| Upload   | `curl -X POST "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt" `         |
| Download | `curl -X GET "http://localhost:8080/api/files/download?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt" ` |
| Update   | `curl -X PUT "http://localhost:8080/api/files?localPath=/path/to/local/file.txt&remoteSrc=/path/in/bucket/file.txt" `          |
| Delete   | `curl -X DELETE "http://localhost:8080/api/files?remoteSrc=/path/in/bucket/file.txt&recursive=false" `                         |
| List     | `curl -X GET "http://localhost:8080/api/files?path=/path/in/bucket" `                                                          |
| Exists   | `curl -X GET "http://localhost:8080/api/files/exists?remoteSrc=/path/in/bucket/file.txt" `                                     |
| Share    | `curl -X POST "http://localhost:8080/api/files/share?remoteSrc=/path/in/bucket/file.txt&expirationTime=3600" `                 |

### Notes

1. Replace `/path/to/local/file.txt` with the actual path inside your container or mapped volume
2. `remoteSrc` is the path in the bucket
3. All endpoints are under `http://localhost:8080/api/files`

## Tips

> You can add the `-v` parameter (`curl -v -X GET "http://localhost:8080/api/files?path="`) if you want to a have a verbose stack trace like this :

```bash
curl -v -X GET "http://localhost:8080/api/files?path="

Note: Unnecessary use of -X or --request, GET is already inferred.
* Host localhost:8080 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying [::1]:8080...
* Connected to localhost (::1) port 8080
* using HTTP/1.x
> GET /api/files?path= HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/8.11.1
> Accept: */*
>
* Request completely sent off
< HTTP/1.1 200
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Mon, 22 Dec 2025 10:10:29 GMT
<
{ [28 bytes data]
100    22    0    22    0     0     90      0 --:--:-- --:--:-- --:--:--    91
* Connection #0 to host localhost left intact
["file.md","file2.md"]%
```

> You can also add `| jq` at the end of the request to format the output in JSON format.

- Example with `| jq` :

```bash
curl -X GET "http://localhost:8080/api/files?path=" | jq
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100    22    0    22    0     0     90      0 --:--:-- --:--:-- --:--:--    90
[
  "file.md",
  "file2.md"
]
```

- Example without `| jq` :

```bash
curl -X GET "http://localhost:8080/api/files?path="
["file.md","file2.md"]
```
