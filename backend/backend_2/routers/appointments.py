from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
import crud, schemas

router = APIRouter(prefix="/appointments", tags=["appointments"])

@router.post("/", response_model=schemas.AppointmentResponse)
def create_appointment(appt: schemas.AppointmentCreate, db: Session = Depends(get_db)):
    # optionally check user/doctor exist
    return crud.create_appointment(db, appt.dict())

@router.get("/user/{user_id}", response_model=list[schemas.AppointmentResponse])
def get_user_appointments(user_id: int, db: Session = Depends(get_db)):
    return crud.list_user_appointments(db, user_id)
