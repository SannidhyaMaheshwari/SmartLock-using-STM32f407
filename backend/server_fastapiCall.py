import os
import numpy as np
from PIL import Image

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from fastapi.middleware.cors import CORSMiddleware

from facenet_pytorch import MTCNN, InceptionResnetV1
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()

# CORS for frontend
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global state
lock_status = 0
temp_password = ""
temp_usage_left = 0

@app.get("/lock_status")
def get_lock_status():
    return {"lock_status": lock_status}

@app.get("/lock")
def lock_door():
    global lock_status
    lock_status = 0
    return {"message": "Door locked", "lock_status": lock_status}

@app.get("/unlock")
def unlock_door():
    global lock_status
    lock_status = 1
    return {"message": "Door unlocked", "lock_status": lock_status}

@app.get("/set_temp_password")
def set_temp_password(value: str):
    global temp_password, temp_usage_left
    if len(value) != 4 or not value.isdigit():
        return {"status": "error", "message": "Temp password must be 4 digits."}
    temp_password = value
    temp_usage_left = 2
    return {"status": "success", "temp_password": temp_password, "usage_left": temp_usage_left}

@app.get("/temp_password")
def get_temp_password():
    return {"temp_password": temp_password, "usage_left": temp_usage_left}



# Initialize face detector and face encoder
mtcnn = MTCNN(image_size=160, margin=0)
resnet = InceptionResnetV1(pretrained='vggface2').eval()

# Directory containing known face images
DATABASE_DIR = "database"

# Load known face encodings
def load_face_encoding(image_path):
    img = Image.open(image_path).convert('RGB')
    face = mtcnn(img)
    if face is not None:
        return resnet(face.unsqueeze(0)).detach().numpy()[0]
    return None

def load_known_faces():
    encodings = {}
    for file in os.listdir(DATABASE_DIR):
        if file.lower().endswith((".jpg", ".jpeg", ".png")):
            name = os.path.splitext(file)[0]
            path = os.path.join(DATABASE_DIR, file)
            encoding = load_face_encoding(path)
            if encoding is not None:
                encodings[name] = encoding
    return encodings

known_faces = load_known_faces()

@app.post("/verify/")
async def verify_user(file: UploadFile = File(...)):
    try:
        img = Image.open(file.file).convert("RGB")
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid image file")

    face = mtcnn(img)
    if face is None:
        raise HTTPException(status_code=400, detail="No face detected in the uploaded image.")

    uploaded_encoding = resnet(face.unsqueeze(0)).detach().numpy()[0]

    min_dist = float("inf")
    matched_name = None
    threshold = 1.4  # Adjust based on your testing

    for name, known_encoding in known_faces.items():
        dist = np.linalg.norm(uploaded_encoding - known_encoding)
        if dist < min_dist:
            min_dist = dist
            matched_name = name
    print(f" distance: {dist}")
    print(f" Minimum distance: {min_dist}")
    
    if min_dist > threshold:
        return JSONResponse(content={"match": "No match found", "distance": float(min_dist)}, status_code=200)
    else:
        return JSONResponse(content={"match": matched_name, "distance": float(min_dist)}, status_code=200)

