from fastapi import FastAPI
import os
from database import engine, Base
from routers import doctors, appointments, documents
import auth
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Medical Appointment API")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
# Créer le dossier uploads
os.makedirs("uploads/chat", exist_ok=True)

# Servir les fichiers uploadés
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")
# Servir les fichiers uploadés
app.mount("/uploads", StaticFiles(directory="uploads"), name="uploads")

# Importer les routers
from routers import chat, chat_files
app.include_router(chat.router)
app.include_router(chat_files.router)

app.include_router(auth.router)
app.include_router(doctors.router)
app.include_router(appointments.router)
app.include_router(documents.router)

# endpoint racine
@app.get("/")
def root():
    return {"msg": "API Medical - FastAPI"}
