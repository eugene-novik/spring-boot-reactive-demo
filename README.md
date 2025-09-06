# Reactive File Proxy & Orchestrator Project

## Overview

This project demonstrates a **reactive microservices setup** for streaming files between services efficiently, using Spring Boot and WebFlux.

There are **two microservices**:

1. **Process Orchestrator (MS2)** – fully reactive, acts as a client-facing orchestrator.
2. **File Service (MS1)** – traditional Spring MVC (Tomcat), streams files from disk using `StreamingResponseBody`.

Files are streamed **chunk-by-chunk**, allowing large files to be transferred without consuming excessive memory.

## Technology Stack

* **Java 24**
* **Spring Boot 3.x**

    * MS2: `spring-boot-starter-webflux`
    * MS1: `spring-boot-starter-web`
* **Lombok**
* **H2 Database** (for storing file metadata: UUID → file path)
* **Logging**: SLF4J / Logback

## Architecture

```
+-------------------+        +--------------------+
| MS2 Orchestrator  |        |  MS1 File Service  |
| Reactive WebFlux  | <----> |  Service (Tomcat)  |
| Port 8081         |        |  Port 8080         |
+-------------------+        +--------------------+
```

1. **Client → MS2**: requests file by UUID.
2. **MS2 → MS1**: reactive HTTP call with WebClient, streams file as `Flux<DataBuffer>`.
3. **MS2 → Client**: streams the file **with all headers preserved** (`Content-Type`, `Content-Disposition`, `Content-Length`).

## File Metadata Example

MS1 stores file metadata in H2:

| UUID                                 | Path                     |
| ------------------------------------ | ------------------------ |
| d56ada6a-ae30-4229-9030-93a3219db85a | static/english-words.pdf |
| fc0d8f9f-4b05-4f83-b318-d7a7c3dae3cb | static/large-file.txt    |

## Running the Services

### File Service (Tomcat)

```bash
cd file-service
./mvnw spring-boot:run
```

* Runs on **port 8080**
* Example endpoint: `GET http://localhost:8080/api/v1/files/{fileID}`
* Streams file in chunks with headers: `Content-Type`, `Content-Disposition`, `Content-Length`.

### Reactive Orchestrator

```bash
cd process-orchestration
./mvnw spring-boot:run
```

* Runs on **port 8081**
* Example endpoint: `GET http://localhost:8081/proxy/{documentID}`
* Fetches file from MS1 reactively, preserves headers, and streams to client.
* Handles 4xx/5xx responses from MS1 and network errors gracefully.

## Features

* **Reactive streaming** of large files without consuming excessive memory
* **Header propagation** (`Content-Type`, `Content-Disposition`, `Content-Length`)
* **Error handling**:

    * 4xx/5xx from MS1 → returned as-is to client
    * Network/streaming errors → 503 / 500
* **Progress logging** of file chunks

## Example Usage

### Request a file

```bash
curl -v http://localhost:8081/proxy/d56ada6a-ae30-4229-9030-93a3219db85a --output english-words.pdf
```

### Large file streaming

```bash
curl -v http://localhost:8081/proxy/fc0d8f9f-4b05-4f83-b318-d7a7c3dae3cb --output large-file.txt
```

* The files are streamed **chunk by chunk**, so memory footprint remains minimal.

## Notes

* MS1 can remain **non-reactive**, only the file streaming part is handled efficiently with `StreamingResponseBody`.
* MS2 is fully reactive (`WebFlux`) and uses `Flux<DataBuffer>` to stream files without blocking threads.
* This setup can be extended to support **authentication, logging, or other orchestration logic** in MS2.
