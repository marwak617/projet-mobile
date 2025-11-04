from faker import Faker
from sqlalchemy.orm import Session
from database import SessionLocal, engine, Base
from models import User, Doctor, Appointment, Document
from datetime import datetime, timedelta
import random

# Créer les tables si elles n'existent pas
Base.metadata.create_all(bind=engine)

# Initialiser Faker
fake = Faker('fr_FR')  # Génère des données françaises
db: Session = SessionLocal()

def seed_users(n=10):
    users = []
    for _ in range(n):
        user = User(
            name=fake.name(),
            email=fake.unique.email(),
            password="hashedpassword123",  # tu peux changer par un vrai hash plus tard
            region=fake.city(),
            mutuelle_url=fake.url()
        )
        users.append(user)
        db.add(user)
    db.commit()
    print(f"✅ {n} utilisateurs ajoutés !")
    return users

def seed_doctors(n=10):
    doctors = []
    specialities = ["Cardiologue", "Dentiste", "Dermatologue", "Généraliste", "Pédiatre", "Ophtalmologue"]
    for _ in range(n):
        doctor = Doctor(
            name=fake.name(),
            speciality=random.choice(specialities),
            city=fake.city(),
            latitude=str(fake.latitude()),
            longitude=str(fake.longitude())
        )
        doctors.append(doctor)
        db.add(doctor)
    db.commit()
    print(f"✅ {n} médecins ajoutés !")
    return doctors

def seed_appointments(users, doctors, n=15):
    for _ in range(n):
        appointment = Appointment(
            user_id=random.choice(users).id,
            doctor_id=random.choice(doctors).id,
            date=fake.date_time_between(start_date='-1y', end_date='+1y'),
            status=random.choice(["pending", "confirmed", "cancelled"])
        )
        db.add(appointment)
    db.commit()
    print(f"✅ {n} rendez-vous ajoutés !")

def seed_documents(users, n=20):
    for _ in range(n):
        doc = Document(
            user_id=random.choice(users).id,
            filename=f"{fake.word()}.pdf",  # génère un nom de fichier simple
            file_url=fake.url(),
            type=random.choice(["ordonnance", "radio", "analyse"])
        )
        db.add(doc)
    db.commit()
    print(f"✅ {n} documents ajoutés !")


def main():
    print("🚀 Démarrage du remplissage de la base de données...")
    users = seed_users(10)
    doctors = seed_doctors(8)
    seed_appointments(users, doctors, 20)
    seed_documents(users, 25)
    print("🎉 Données de test insérées avec succès !")

if __name__ == "__main__":
    main()
