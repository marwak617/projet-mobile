from fastapi import FastAPI
from database import engine, Base
from routers import doctors, appointments, documents
import auth
# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Medical Appointment API")

app.include_router(auth.router)
app.include_router(doctors.router)
app.include_router(appointments.router)
app.include_router(documents.router)

# endpoint racine
@app.get("/")
def root():
    return {"msg": "API Medical - FastAPI"}
