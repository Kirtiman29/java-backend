# Bitmap Frontend Integration

This document describes the frontend contract for bitmap upload, preview, and export.

## What The Frontend Must Call

All bitmap requests must go through the subscription-service endpoint:

`/api/bitmap`

Example base URL in local development:

`http://localhost:8094/api/bitmap`

Do not call `https://localhost:3000/bitmap/...` unless your frontend dev server explicitly proxies that path to the backend. The backend controller is mounted at `/api/bitmap`.

## Authentication

Send the JWT in the `Authorization` header:

`Authorization: Bearer <access-token>`

The backend uses the JWT subject as the user id.

## Access Rules

Bitmap routes require:

- JWT authentication
- `ROLE_USER`
- an active subscription

Bitmap endpoints do **not** deduct AI credits and do **not** consume design quota.

## Recommended Frontend Configuration

Use a single configurable base URL for bitmap requests.

### Environment

```env
VITE_BITMAP_SERVICE_URL=http://localhost:8094/api/bitmap
```

### Dev fallback

If you prefer same-origin dev requests, proxy `/api/bitmap` from the frontend dev server to `http://localhost:8094`.

Important: use `/api/bitmap`, not `/bitmap`.

## Frontend Upload Flow

1. User selects an image file.
2. Frontend creates `FormData`.
3. Frontend appends the file under the field name `file`.
4. Frontend sends `POST /api/bitmap/upload`.
5. Backend returns a stored filename plus bitmap metadata.
6. Frontend stores the returned `filename` and uses it for all later preview/export calls.

There is no list endpoint for bitmap files, so the upload response must be kept in UI state.

## Upload Endpoint

`POST /api/bitmap/upload`

Request:

- Content type: `multipart/form-data`
- Field name: `file`

Example:

```javascript
const formData = new FormData();
formData.append("file", file);

const res = await fetch(`${BITMAP_BASE_URL}/upload`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
  },
  body: formData,
});

if (!res.ok) {
  throw new Error(await res.text());
}

const data = await res.json();
```

Expected response:

```json
{
  "message": "Upload successful",
  "filename": "uuid_originalname.png",
  "path": "/some/storage/path",
  "sizeBytes": 123456,
  "bitmapId": 42
}
```

Frontend notes:

- Store `filename` from the response.
- Do not reuse the original client filename for later requests.
- `bitmapId` is optional UI state, but `filename` is required for preview/export.

## Analyze Endpoint

`POST /api/bitmap/analyze`

Request:

- Send `filename` as a form field or URL-encoded parameter

Example:

```javascript
const params = new URLSearchParams();
params.append("filename", storedFilename);

const res = await fetch(`${BITMAP_BASE_URL}/analyze`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/x-www-form-urlencoded",
  },
  body: params,
});

const data = await res.json();
```

Expected response:

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

Frontend notes:

- Always pass the stored `filename`.
- The backend updates stored bitmap metadata after analysis succeeds.

## Preview And Export Endpoints

These routes all require `filename` and return binary data:

- `POST /api/bitmap/preview/halftone`
- `POST /api/bitmap/preview/dither`
- `POST /api/bitmap/preview/separation-proof`
- `POST /api/bitmap/export/separation-zip`
- `POST /api/bitmap/export/psd`
- `POST /api/bitmap/export/cmyk`

Use `fetch(...).blob()` or Axios `responseType: "blob"` for these requests.

Example:

```javascript
const params = new URLSearchParams();
params.append("filename", storedFilename);

const res = await fetch(`${BITMAP_BASE_URL}/preview/halftone`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/x-www-form-urlencoded",
  },
  body: params,
});

if (!res.ok) {
  throw new Error(await res.text());
}

const blob = await res.blob();
```

## Gemini Image-To-Image Endpoint

`POST /api/bitmap/gemini-image/image-to-image`

Request:

- `multipart/form-data`
- Required field: `file`
- Optional fields: `edit_mode`, `edit_type`, `source_color`, `target_color`, `color_preset`, `color_palette`, `mask_file`, `num_images`, `aspect_ratio`

Use this endpoint for textile recolor workflows, palette presets, and masked replacements.

Example:

```javascript
const formData = new FormData();
formData.append("file", imageFile);
formData.append("edit_mode", "precise_edit");
formData.append("edit_type", "change color");
formData.append("source_color", "#c73542");
formData.append("target_color", "#1f7a5c");
formData.append("num_images", "1");
formData.append("aspect_ratio", "1:1");

const res = await fetch(`${BITMAP_BASE_URL}/gemini-image/image-to-image`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
  },
  body: formData,
});

if (!res.ok) {
  throw new Error(await res.text());
}

const data = await res.json();
const outputUrl = data.output_url ? `${BITMAP_BASE_URL}${data.output_url}` : null;
```

Frontend notes:

- Prefer `data.output_url` when a single image is returned.
- Use `data.image_urls` when the backend returns multiple images.
- Prepend the backend base URL because the returned URLs are relative.

## Textile Control Panel Reference

This section captures the React left-panel workflow from the pasted spec. It is the recommended implementation shape for a textile recolor panel that lets the user upload an image, pick a quick preset, choose a background color, and optionally add a prompt before generating.

Important: in this repository, point the frontend at `subscription-service` through `VITE_BITMAP_SERVICE_URL` unless you are intentionally wiring a standalone FastAPI deployment. The contract below is the same either way, but the base URL changes.

### Suggested Files

```txt
src/
  types/
    textile.ts
  api/
    imageToImageApi.ts
  components/
    TextileControlPanel.tsx
    TextileControlPanel.css
```

### Shared Types

Keep the request and response shape explicit so the panel stays easy to reason about.

```ts
export type PresetId =
  | "monotone"
  | "pastel"
  | "dark"
  | "dark_warm"
  | "dark_cool"
  | "dark_gray"
  | "dark_khaki"
  | "dusty"
  | "dark_dusty"
  | "fresh"
  | "candy"
  | "ice_cream"
  | "kids"
  | "home_decor"
  | "home_furnishing"
  | "wallpaper"
  | "earthy"
  | "mens_formal"
  | "mens_casual"
  | "mens_party"
  | "mens_ethnic"
  | "womens_formal"
  | "womens_casual"
  | "womens_party";

export type GenerateResponse = {
  output_url?: string;
  image_urls?: string[];
  edit_mode?: string;
  edit_options?: Record<string, unknown>;
  model?: string;
  prompt_enhanced?: boolean;
  fallback_used?: boolean;
  final_prompt?: string;
};
```

### Request Builder

Build the request with `FormData`. Do not set `Content-Type` manually.

```ts
const API_BASE_URL =
  import.meta.env.VITE_BITMAP_SERVICE_URL ?? "http://localhost:8094/api/bitmap";

export function resolveImageUrl(url?: string) {
  if (!url) return "";
  if (/^(https?:|data:|blob:)/.test(url)) return url;
  return `${API_BASE_URL}${url}`;
}

export async function generateImage(form: FormData) {
  const response = await fetch(`${API_BASE_URL}/gemini-image/image-to-image`, {
    method: "POST",
    body: form,
  });

  const data = await response.json();
  if (!response.ok) {
    throw new Error(data.detail || data.message || `Request failed with status ${response.status}`);
  }
  return data;
}
```

### Form Flow

The panel should keep the user flow small and obvious:

1. Upload the textile image.
2. Open a preset category and pick a palette.
3. Choose a background color.
4. Optionally type a prompt.
5. Click Generate to send `multipart/form-data`.

### UI Rules

- Require `file` before generation.
- Keep `prompt` optional.
- Disable the Generate button while the request is running.
- Show `image_urls` as a grid when multiple images come back.
- Fall back to `output_url` when there is only one image.
- Keep the uploaded file preview visible so the user can confirm what was sent.

### Validation Checklist

- Allow only `PNG`, `JPG`, `JPEG`, and `WEBP`.
- Validate that the image file exists before submit.
- Keep the preset selector visible for preset-based recolor flows.
- Make sure the background color is a valid hex color.
- Use backend `detail` or `message` fields for error text.
- Reuse the stored base URL when resolving returned relative URLs.

## Gemini Image-To-Image Page Guide

Use this section to build the frontend page for textile recolor, color preset, and masked replacement workflows.

### Page Goal

The page should let a logged-in subscriber:

1. Upload a source image.
2. Optionally upload a mask image.
3. Choose a recolor mode or preset.
4. Submit the request to the subscription-service.
5. Preview the generated output image or images.

### Routing And Access

- Protect the page behind authentication.
- Require an active subscription before allowing generation.
- If your app already has a shared bitmap area, add this screen there instead of creating a separate route.

Suggested route:

`/bitmap/gemini-image`

### Recommended UI Sections

- Source image upload
- Optional mask upload
- Color controls
- Preset controls
- Generation settings
- Output preview area

### Recommended Form State

Keep the page state simple and explicit.

```javascript
const [sourceFile, setSourceFile] = useState(null);
const [maskFile, setMaskFile] = useState(null);
const [editMode, setEditMode] = useState("precise_edit");
const [editType, setEditType] = useState("change color");
const [sourceColor, setSourceColor] = useState("");
const [targetColor, setTargetColor] = useState("");
const [colorPreset, setColorPreset] = useState("");
const [colorPalette, setColorPalette] = useState("");
const [numImages, setNumImages] = useState("1");
const [aspectRatio, setAspectRatio] = useState("1:1");
const [loading, setLoading] = useState(false);
const [error, setError] = useState("");
const [result, setResult] = useState(null);
```

### Suggested UI Rules

- Show `file` as the required upload field.
- Show `mask_file` only when the user selects masked recolor.
- Allow `source_color` and `target_color` as manual hex inputs or color pickers.
- Show `color_palette` when the user selects `pastel` or `dark`.
- Default `edit_mode` to `precise_edit` for color workflows.
- Default `edit_type` to `change color`.
- If the user chooses `white`, `monotone`, `pastel`, or `dark`, keep the preset selector visible.

### Validation Before Submit

Validate on the client before sending the request.

- Source image is required.
- If `mask_file` is chosen, it must be an image file.
- `color_palette` is required for `pastel` and `dark`.
- `source_color` and `target_color` should look like hex values if entered manually.
- `num_images` should be a positive integer.

### Request Build Flow

Build the request as `FormData`.

```javascript
const formData = new FormData();

formData.append("file", sourceFile);
formData.append("edit_mode", editMode);
formData.append("edit_type", editType);

if (sourceColor) formData.append("source_color", sourceColor);
if (targetColor) formData.append("target_color", targetColor);
if (colorPreset) formData.append("color_preset", colorPreset);
if (colorPalette) formData.append("color_palette", colorPalette);
if (maskFile) formData.append("mask_file", maskFile);
if (numImages) formData.append("num_images", numImages);
if (aspectRatio) formData.append("aspect_ratio", aspectRatio);

const res = await fetch(`${BITMAP_BASE_URL}/gemini-image/image-to-image`, {
  method: "POST",
  headers: {
    Authorization: `Bearer ${token}`,
  },
  body: formData,
});
```

Do not set the `Content-Type` header manually. The browser must add the multipart boundary.

### Response Handling

The backend may return one or more output images.

If `output_url` is present:

- Treat it as the primary generated image.
- Prepend the subscription-service base URL to render it in the browser.

If `image_urls` is present:

- Render all returned images in a gallery or grid.
- Preserve order, because the backend may return the most relevant image first.

Example display logic:

```javascript
const outputUrls = result?.image_urls?.length
  ? result.image_urls
  : result?.output_url
    ? [result.output_url]
    : [];

const resolvedUrls = outputUrls.map((path) => `${BITMAP_BASE_URL}${path}`);
```

### Suggested Output UI

- Show the generated image as soon as the response arrives.
- Include a download button for each returned URL.
- Show the final prompt for debugging or traceability.
- Show the selected source and target colors so users can confirm what was sent.

### Mask Workflow

When a mask is supplied:

- White or bright regions are editable.
- Black or dark regions should remain unchanged.

Recommended UI copy:

- "White areas will be recolored."
- "Black areas will be preserved."

### Preset Workflow

Use these supported presets:

- `monotone`
- `pastel`
- `dark`
- `dark_dusty`
- `natural`
- `white`
- `natural_white`

Preset guidance:

- `pastel` and `dark` require `color_palette`.
- `white` is useful when the user wants the background or base to become white.
- `monotone` works best when paired with a single strong target color.

### Error States

Show clear errors for:

- Missing source image
- Invalid image type
- Missing palette for `pastel` or `dark`
- No active subscription
- Backend timeout
- Backend validation error

Recommended handling:

- Clear the loading state on failure.
- Keep the selected form values so the user can retry.
- Show the backend `message` field if the server returns one.

### Loading States

The generation request can take time, so provide feedback.

- Disable the submit button while loading.
- Show a spinner or progress label.
- Prevent duplicate submissions.

### Minimal Page Contract

At minimum, the frontend page should support:

- `file`
- `edit_mode`
- `edit_type`
- `target_color`
- `color_preset`
- `color_palette`
- `mask_file`
- `num_images`
- `aspect_ratio`

### Example Page Behavior

1. User uploads a textile image.
2. User picks `precise_edit`.
3. User sets `edit_type` to `change color`.
4. User either picks a preset or enters a target color.
5. User optionally uploads a mask.
6. User clicks Generate.
7. Page renders `output_url` or all `image_urls`.
8. User downloads or reuses the generated image.

## Error Handling

The backend returns `400 Bad Request` for validation and runtime failures, including:

- Missing `filename`
- File not owned by the logged-in user
- No active subscription
- Bitmap backend unavailable

Suggested UI handling:

- Show a subscription-required message for access denial
- Show validation errors inline
- Show a retry banner or toast when the bitmap backend is unavailable

Example error body:

```json
{
  "timestamp": "2026-06-29T11:42:59.659",
  "status": 400,
  "error": "Bad Request",
  "message": "File not found or access denied",
  "path": "/api/bitmap/analyze"
}
```

## Correct Frontend Service Shape

```javascript
const BITMAP_BASE_URL =
  import.meta.env.VITE_BITMAP_SERVICE_URL || "http://localhost:8094/api/bitmap";

const authHeaders = (token) =>
  token ? { Authorization: `Bearer ${token}` } : {};

export const bitmapApi = {
  async upload(file, token) {
    const formData = new FormData();
    formData.append("file", file);

    const res = await fetch(`${BITMAP_BASE_URL}/upload`, {
      method: "POST",
      headers: {
        ...authHeaders(token),
      },
      body: formData,
    });

    if (!res.ok) throw new Error(await res.text());
    return await res.json();
  },

  async analyze(filename, token) {
    const body = new URLSearchParams({ filename });
    const res = await fetch(`${BITMAP_BASE_URL}/analyze`, {
      method: "POST",
      headers: {
        ...authHeaders(token),
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body,
    });

    if (!res.ok) throw new Error(await res.text());
    return await res.json();
  },

  async previewHalftone(params, token) {
    const body = new URLSearchParams(params);
    const res = await fetch(`${BITMAP_BASE_URL}/preview/halftone`, {
      method: "POST",
      headers: {
        ...authHeaders(token),
        "Content-Type": "application/x-www-form-urlencoded",
      },
      body,
    });

    if (!res.ok) throw new Error(await res.text());
    return await res.blob();
  },
};
```

## Quick Checklist

- User is authenticated with JWT
- User has an active subscription
- Request URL starts with `/api/bitmap`
- Upload uses `FormData` with field name `file`
- Upload response `filename` is stored
- Later requests reuse the stored `filename`
- Binary responses are handled as blobs
