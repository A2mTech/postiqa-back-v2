# API Documentation

## Overview

Postiqa Back v2 provides two REST APIs:
- **Business API** (`/api/business/*`): For businesses managing their own social media
- **Agency API** (`/api/agency/*`): For agencies managing multiple client accounts

Both APIs are secured with JWT authentication and follow RESTful conventions.

## Authentication

### JWT Bearer Token

All endpoints require authentication via Bearer token in the `Authorization` header:

```http
Authorization: Bearer <access_token>
```

**Token Acquisition**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response**:
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

**Token Refresh**:
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}
```

### API Key Authentication

For machine-to-machine communication:

```http
X-API-Key: your_api_key_here
```

### OAuth2 Social Login

Supported providers: Google, GitHub, LinkedIn

```http
GET /oauth2/authorization/{provider}
```

## Business API

**Base URL**: `/api/business`

**Audience**: Businesses with single organization

### Organization Management

#### Invite Member
```http
POST /api/business/organization/members/invite
Content-Type: application/json

{
  "email": "member@example.com",
  "roleId": "uuid",
  "permissions": ["POST:CREATE", "POST:UPDATE"]
}
```

#### Create Member Directly
```http
POST /api/business/organization/members/create
Content-Type: application/json

{
  "email": "member@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roleId": "uuid"
}
```

#### List Organization Members
```http
GET /api/business/organization/{organizationId}/members
```

**Response**:
```json
[
  {
    "id": "uuid",
    "userId": "uuid",
    "email": "member@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": {
      "id": "uuid",
      "name": "MEMBER",
      "permissions": ["POST:CREATE", "POST:READ"]
    },
    "joinedAt": "2025-01-15T10:30:00Z"
  }
]
```

#### Update Member
```http
PUT /api/business/organization/{organizationId}/members/{memberId}
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith"
}
```

#### Update Member Role
```http
PUT /api/business/organization/{organizationId}/members/{memberId}/role
Content-Type: application/json

{
  "roleId": "uuid"
}
```

#### Remove Member
```http
DELETE /api/business/organization/{organizationId}/members/{memberId}
```

#### Grant Permission Override
```http
POST /api/business/organization/permissions/grant
Content-Type: application/json

{
  "userId": "uuid",
  "permission": "POST:DELETE",
  "grant": true
}
```

#### Get Activity Logs
```http
GET /api/business/organization/{organizationId}/activity-logs?page=0&size=20
```

### Social Accounts

#### Generate OAuth Authorization URL
```http
GET /api/business/social-accounts/authorize/{platform}
```

**Platforms**: `linkedin`, `twitter`, `instagram`, `youtube`, `tiktok`

**Response**:
```json
{
  "authorizationUrl": "https://www.linkedin.com/oauth/v2/authorization?...",
  "state": "random_state_value"
}
```

#### Connect Social Account (OAuth Callback)
```http
POST /api/business/social-accounts/callback
Content-Type: application/json

{
  "platform": "linkedin",
  "code": "auth_code_from_provider",
  "state": "random_state_value"
}
```

**Response**:
```json
{
  "id": "uuid",
  "platform": "LINKEDIN",
  "platformUserId": "linkedin_user_id",
  "username": "john_doe",
  "displayName": "John Doe",
  "profileImageUrl": "https://...",
  "isActive": true,
  "connectedAt": "2025-01-15T10:30:00Z",
  "tokenExpired": false
}
```

#### List Social Accounts
```http
GET /api/business/social-accounts?platform=linkedin&activeOnly=true
```

#### Get Social Account
```http
GET /api/business/social-accounts/{accountId}
```

#### Disconnect Social Account
```http
DELETE /api/business/social-accounts/{accountId}
```

#### Refresh Access Token
```http
POST /api/business/social-accounts/{accountId}/refresh
```

#### Test Connection
```http
POST /api/business/social-accounts/{accountId}/test
```

**Response**:
```json
{
  "success": true,
  "message": "Connection is valid"
}
```

### Posts

#### Create Post
```http
POST /api/business/posts
Content-Type: application/json

{
  "content": "Check out our new product! #launch",
  "channels": ["channel_id_1", "channel_id_2"],
  "mediaIds": ["media_id_1"],
  "status": "DRAFT"
}
```

**Response**:
```json
{
  "id": "uuid",
  "content": "Check out our new product! #launch",
  "status": "DRAFT",
  "channels": [
    {
      "id": "uuid",
      "socialAccountId": "uuid",
      "platform": "LINKEDIN"
    }
  ],
  "media": [
    {
      "id": "uuid",
      "type": "IMAGE",
      "url": "https://...",
      "thumbnailUrl": "https://..."
    }
  ],
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

#### Get Post
```http
GET /api/business/posts/{postId}
```

#### List Posts
```http
GET /api/business/posts?status=SCHEDULED&page=0&size=20
```

**Query Parameters**:
- `status`: Filter by status (`DRAFT`, `SCHEDULED`, `PUBLISHED`, `FAILED`)
- `page`: Page number (0-indexed)
- `size`: Items per page

#### Update Post
```http
PUT /api/business/posts/{postId}
Content-Type: application/json

{
  "content": "Updated content",
  "channels": ["channel_id_1"],
  "mediaIds": []
}
```

#### Delete Post
```http
DELETE /api/business/posts/{postId}
```

#### Schedule Post
```http
POST /api/business/posts/{postId}/schedule
Content-Type: application/json

{
  "scheduledAt": "2025-01-20T14:00:00Z"
}
```

#### Cancel Schedule
```http
POST /api/business/posts/{postId}/cancel
```

### Channels

Channels represent social accounts configured for posting.

#### Get Channel
```http
GET /api/business/channels/{channelId}
```

#### List Channels
```http
GET /api/business/channels?platform=linkedin&activeOnly=true
```

**Response**:
```json
[
  {
    "id": "uuid",
    "socialAccountId": "uuid",
    "platform": "LINKEDIN",
    "username": "john_doe",
    "displayName": "John Doe",
    "isActive": true
  }
]
```

### Media

#### Upload Media
```http
POST /api/business/posts/{postId}/media
Content-Type: multipart/form-data

file: <binary>
```

**Response**:
```json
{
  "id": "uuid",
  "type": "IMAGE",
  "url": "https://storage.example.com/media/uuid.jpg",
  "thumbnailUrl": "https://storage.example.com/media/uuid_thumb.jpg",
  "filename": "image.jpg",
  "fileSize": 1024000,
  "mimeType": "image/jpeg",
  "uploadedAt": "2025-01-15T10:30:00Z"
}
```

#### Delete Media
```http
DELETE /api/business/posts/{postId}/media/{mediaId}
```

## Agency API

**Base URL**: `/api/agency`

**Audience**: Agencies managing multiple clients

### Key Differences from Business API

1. **Client Scoping**: All post/channel endpoints require `{clientId}`
2. **Multi-Client Views**: Agency-wide social account listing
3. **Client Access Management**: Delegate client access to members

### Organization Management

All endpoints identical to Business API with these additions:

#### List Members with Client Filter
```http
GET /api/agency/organization/{organizationId}/members?clientId=uuid
```

#### Update Member Role with Client Scope
```http
PUT /api/agency/organization/{organizationId}/members/{memberId}/role
Content-Type: application/json

{
  "roleId": "uuid",
  "clientId": "uuid"
}
```

#### Delegate Client Access (TODO)
```http
PUT /api/agency/organization/{organizationId}/members/{memberId}/clients
Content-Type: application/json

{
  "clientIds": ["uuid1", "uuid2"]
}
```

#### Activity Logs with Client Filter
```http
GET /api/agency/organization/{organizationId}/activity-logs?clientId=uuid&page=0&size=20
```

### Social Accounts

#### Generate OAuth URL (Client-Scoped)
```http
GET /api/agency/clients/{clientId}/social-accounts/authorize/{platform}
```

#### Connect Social Account (Client-Scoped)
```http
POST /api/agency/clients/{clientId}/social-accounts/callback
Content-Type: application/json

{
  "platform": "linkedin",
  "code": "auth_code",
  "state": "state_value"
}
```

#### List Client Social Accounts
```http
GET /api/agency/clients/{clientId}/social-accounts
```

#### List All Agency Social Accounts
```http
GET /api/agency/social-accounts?clientId=uuid
```

#### Get Social Account
```http
GET /api/agency/social-accounts/{accountId}
```

#### Disconnect Social Account
```http
DELETE /api/agency/social-accounts/{accountId}
```

### Posts (Client-Scoped)

All post endpoints require `{clientId}` in path:

#### Create Post
```http
POST /api/agency/clients/{clientId}/posts
Content-Type: application/json

{
  "content": "Client post content",
  "channels": ["channel_id"],
  "status": "DRAFT"
}
```

#### Get Post
```http
GET /api/agency/clients/{clientId}/posts/{postId}
```

#### List Posts
```http
GET /api/agency/clients/{clientId}/posts?status=SCHEDULED
```

#### Update Post
```http
PUT /api/agency/clients/{clientId}/posts/{postId}
```

#### Delete Post
```http
DELETE /api/agency/clients/{clientId}/posts/{postId}
```

#### Schedule Post
```http
POST /api/agency/clients/{clientId}/posts/{postId}/schedule
```

#### Cancel Schedule
```http
POST /api/agency/clients/{clientId}/posts/{postId}/cancel
```

### Channels (Client-Scoped)

#### Get Channel
```http
GET /api/agency/clients/{clientId}/channels/{channelId}
```

#### List Channels
```http
GET /api/agency/clients/{clientId}/channels?platform=linkedin
```

## Common Response Codes

| Code | Meaning | Description |
|------|---------|-------------|
| 200  | OK | Request successful |
| 201  | Created | Resource created successfully |
| 204  | No Content | Request successful, no response body |
| 400  | Bad Request | Invalid request payload |
| 401  | Unauthorized | Missing or invalid authentication |
| 403  | Forbidden | Insufficient permissions |
| 404  | Not Found | Resource not found |
| 409  | Conflict | Resource already exists or conflict |
| 422  | Unprocessable Entity | Validation errors |
| 500  | Internal Server Error | Server error |

## Error Response Format

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/business/posts",
  "errors": [
    {
      "field": "content",
      "message": "Content cannot be empty"
    }
  ]
}
```

## Rate Limiting

Currently not implemented. Consider implementing rate limiting before production:
- Per-user limits (e.g., 100 req/min)
- Per-organization limits (e.g., 1000 req/min)
- Per-API-key limits

## Pagination

List endpoints support pagination:

**Query Parameters**:
- `page`: Page number (0-indexed, default: 0)
- `size`: Items per page (default: 20, max: 100)

**Response Headers**:
```
X-Total-Count: 150
X-Total-Pages: 8
X-Current-Page: 0
```

## Filtering & Sorting

**Common Filters**:
- `status`: Filter by resource status
- `platform`: Filter by social platform
- `activeOnly`: Show only active resources

**Sorting** (if implemented):
- `sort`: Field name (e.g., `createdAt`)
- `direction`: `ASC` or `DESC`

## Security Headers

All responses include security headers:

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

## CORS Configuration

**Current**: Wildcard allowed origins (development only)

**Production**: Configure specific allowed origins in `application.properties`:
```properties
cors.allowed-origins=https://app.postiqa.com,https://agency.postiqa.com
```

## Webhook Events (Future)

Planned webhook support for:
- Post published successfully
- Post publish failed
- Social account disconnected
- Token expired

## SDK & Client Libraries

No official SDKs yet. OpenAPI specification planned for auto-generating client libraries.

## Postman Collection

TODO: Export Postman collection for easy API testing.

## Testing Endpoints

For development/testing:

**Health Check**:
```http
GET /actuator/health
```

**Module Info** (Spring Modulith):
```http
GET /actuator/modulith
```
