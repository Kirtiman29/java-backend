# Bitmap FastAPI Contract

This document describes the bitmap worker API that `subscription-service` forwards to.

The FastAPI service is the internal bitmap processing backend. The Java subscription service calls it directly, and the frontend should not call it itself.

## Base URL

Local default:

`http://localhost:8002`

The Java service reads this from:

`BITMAP_BACKEND_URL`

If that environment variable is not set, the subscription service falls back to `http://localhost:8002`.

## Required Header

Every request sent from the Java service includes:

`X-User-Id: <numeric-user-id>`

Use this for ownership checks, logging, or user-scoped storage.

## Endpoint Summary

The FastAPI service should expose these routes:

- `POST /upload/`
- `POST /analyze/`
- `POST /halftone/monochrome`
- `POST /dither/`
- `POST /halftone/separation/proof`
- `POST /halftone/separation`
- `POST /halftone/separation/psd`
- `POST /halftone/cmyk/`
- `POST /gemini-image/image-to-image`

Important:

- The Java client uses the trailing-slash versions shown above.
- Keep these routes stable to avoid redirect issues.

## 1. Upload

`POST /upload/`

Request type:

- `multipart/form-data`

Expected form field:

- `file`

The Java service sends only the uploaded file and the `X-User-Id` header.

Recommended behavior:

- Save the uploaded file to disk or object storage
- Generate a unique stored filename
- Keep the original filename for display metadata
- Return the stored filename in the response

Example response:

```json
{
  "message": "Upload successful",
  "filename": "uuid_originalname.png",
  "path": "/some/storage/path",
  "sizeBytes": 123456,
  "bitmapId": 42
}
```

Response fields expected by the Java service:

- `message`
- `filename`
- `path`
- `sizeBytes`
- `bitmapId`

## 2. Analyze

`POST /analyze/`

Request type:

- Query parameters

Required query parameter:

- `filename`

The Java service sends the filename in the query string, not in the body.

Example request:

`POST /analyze/?filename=uuid_originalname.png`

Example response:

```json
{
  "status": "success",
  "filename": "uuid_originalname.png",
  "designType": "poster",
  "confidence": 0.92,
  "suggestedStyles": ["modern", "minimal"],
  "analysis": {
    "edgeDensityScore": 0.71,
    "colorSaturationScore": 0.63
  }
}
```

Response fields expected by the Java service:

- `status`
- `filename`
- `designType`
- `confidence`
- `suggestedStyles`
- `analysis.edgeDensityScore`
- `analysis.colorSaturationScore`

## 3. Preview And Export

The Java subscription service forwards all bitmap preview/export requests as POST requests with query parameters.

These routes should return binary data:

- `POST /halftone/monochrome`
- `POST /dither/`
- `POST /halftone/separation/proof`
- `POST /halftone/separation`
- `POST /halftone/separation/psd`
- `POST /halftone/cmyk/`

### Shared Request Pattern

Request type:

- `POST`
- Query parameters

Required parameter:

- `filename`

The Java layer may forward additional parameters such as:

- `spacing`
- `dot_size`
- `angle`
- `shape`
- `grayscale_mode`
- `brightness`
- `contrast`
- `preprocess`
- `edge_strength`
- `ink_boost`
- `background_cleanup`
- `binarize`
- `dpi`
- `algorithm`
- `threshold_val`
- `workflow_mode`
- `dot_screen_enabled`
- `spot_color_count`
- `manual_spot_colors`

Recommended behavior:

- Treat `filename` as mandatory
- Accept extra parameters when possible
- Ignore unknown parameters instead of failing unless they are required for your algorithm

### Binary Response

Return the rendered file as raw bytes.

Set an appropriate content type such as:

- `image/png`
- `image/jpeg`
- `application/zip`
- `application/octet-stream`
- `application/pdf`

depending on the output.

For downloadable files, set `Content-Disposition` if you want the browser to suggest a filename.

## 4. Gemini Image-to-Image Color Recolor

`POST /gemini-image/image-to-image`

Request type:

- `multipart/form-data`

Required form field:

- `file`

Optional form fields:

- `edit_mode`
- `edit_type`
- `source_color`
- `target_color`
- `color_preset`
- `color_palette`
- `mask_file`
- `num_images`
- `aspect_ratio`
- any additional Gemini edit options the caller forwards

Recommended values for color work:

- `edit_mode = precise_edit`
- `edit_type = change color`

Response type:

- JSON

The caller expects the FastAPI response to include:

- `success`
- `message`
- `input_prompt`
- `final_prompt`
- `source_color`
- `target_color`
- `edit_type`
- `model`
- `aspect_ratio`
- `num_images`
- `edit_mode`
- `edit_options`
- `mask_used`
- `filenames`
- `image_urls`
- `filename`
- `output_url`

The Java subscription service forwards this response to the frontend unchanged.

## Error Handling

Use standard FastAPI error responses:

- `400` for invalid parameters or missing files
- `404` for missing stored assets
- `500` for unexpected failures

Example JSON error:

```json
{
  "detail": "File not found"
}
```

The Java service forwards FastAPI error bodies back to the frontend, so keep the response body concise and readable.

## Implementation Notes

- The Java service already validates subscription state before calling FastAPI
- The Java service already validates file ownership before preview/export calls
- FastAPI does not need to repeat subscription checks unless you want defense in depth
- FastAPI should still verify the incoming `X-User-Id` if it uses that header for access control or storage partitioning
- Keep processing deterministic where possible so repeated requests with the same input produce stable output

## Suggested FastAPI Shape

```python
from fastapi import FastAPI, File, UploadFile, Request
from fastapi.responses import Response, JSONResponse

app = FastAPI()

@app.post("/upload/")
async def upload(file: UploadFile = File(...), request: Request = None):
    ...

@app.post("/analyze/")
async def analyze(filename: str, request: Request = None):
    ...

@app.post("/halftone/monochrome")
async def halftone_monochrome(filename: str, request: Request = None):
    ...
```

## Integration Checklist

- FastAPI runs on the URL configured in `BITMAP_BACKEND_URL`
- `POST /upload/` accepts multipart file uploads with the `file` field
- `POST /analyze/` accepts `filename` as a query parameter
- Preview/export routes accept query parameters and return binary bytes
- `X-User-Id` is read and preserved for ownership or logging
- Route names include the trailing slashes where the Java service expects them
