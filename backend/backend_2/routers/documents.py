import os
from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from sqlalchemy.orm import Session
from database import get_db
import crud

router = APIRouter(prefix="/documents", tags=["documents"])

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

@router.post("/upload")
async def upload_document(user_id: int, file: UploadFile = File(...), db: Session = Depends(get_db)):
    filename = file.filename
    filepath = os.path.join(UPLOAD_DIR, filename)

    with open(filepath, "wb") as f:
        content = await file.read()
        f.write(content)

    file_url = f"/{UPLOAD_DIR}/{filename}"  # pour tests locaux
    doc = crud.save_document(db, user_id=user_id, filename=filename, file_url=file_url)
    return {"filename": filename, "file_url": file_url, "id": doc.id}
