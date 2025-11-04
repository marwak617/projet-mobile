from sqlalchemy.orm import Session
from models import User, Doctor, Appointment, Document

# Users
def get_user_by_email(db: Session, email: str):
    return db.query(User).filter(User.email == email).first()

def create_user(db: Session, name: str, email: str, password: str, region: str = None):
    # Plus de hachage, stocker le mot de passe en clair
    user = User(name=name, email=email, password=password, region=region)
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

def get_user(db: Session, user_id: int):
    return db.query(User).filter(User.id == user_id).first()

# Doctors
def create_doctor(db: Session, doctor_data):
    doc = Doctor(**doctor_data)
    db.add(doc)
    db.commit()
    db.refresh(doc)
    return doc

def list_doctors(db: Session, skip: int = 0, limit: int = 100):
    return db.query(Doctor).offset(skip).limit(limit).all()

# Appointments
def create_appointment(db: Session, appointment_data):
    appt = Appointment(**appointment_data)
    db.add(appt)
    db.commit()
    db.refresh(appt)
    return appt

def list_user_appointments(db: Session, user_id: int):
    return db.query(Appointment).filter(Appointment.user_id == user_id).all()

# Documents
def save_document(db: Session, user_id: int, filename: str, file_url: str, type: str = None):
    doc = Document(user_id=user_id, filename=filename, file_url=file_url, type=type)
    db.add(doc)
    db.commit()
    db.refresh(doc)
    return doc
