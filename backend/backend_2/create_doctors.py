# create_doctors.py
from sqlalchemy.orm import Session
from database import SessionLocal
from models import User

def create_test_doctors():
    db = SessionLocal()
    
    doctors_data = [
        {"name": "Ahmed Bennani", "email": "bennani@hospital.ma", "password": "123456", 
         "role": "doctor", "specialty": "Cardiologue", "region": "Casablanca", "phone": "+212 6 12 34 56 78"},
        {"name": "Fatima Alaoui", "email": "alaoui@clinic.ma", "password": "123456", 
         "role": "doctor", "specialty": "Pédiatre", "region": "Rabat", "phone": "+212 6 23 45 67 89"},
        {"name": "Mohamed Tazi", "email": "tazi@medical.ma", "password": "123456", 
         "role": "doctor", "specialty": "Dentiste", "region": "Marrakech", "phone": "+212 6 34 56 78 90"},
        {"name": "Leila Idrissi", "email": "idrissi@health.ma", "password": "123456", 
         "role": "doctor", "specialty": "Dermatologue", "region": "Fès", "phone": "+212 6 45 67 89 01"},
    ]
    
    for doc_data in doctors_data:
        doctor=User(**doc_data)
        db.add(doctor)
        
    
    db.commit()
    print("✅ Médecins créés avec succès!")
    db.close()

if __name__ == "__main__":
    create_test_doctors()