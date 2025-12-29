# utils/file_validator.py
from PIL import Image
import magic
import os

def validate_image_content(file_path: str) -> bool:
    """Valide que le fichier est vraiment une image"""
    try:
        with Image.open(file_path) as img:
            img.verify()
        return True
    except:
        return False

def get_real_mime_type(file_path: str) -> str:
    """Obtenir le vrai type MIME du fichier"""
    mime = magic.Magic(mime=True)
    return mime.from_file(file_path)

def scan_for_malware(file_path: str) -> bool:
    """Scanner le fichier pour les malwares (intégrer avec ClamAV)"""
    # TODO: Implémenter selon vos besoins
    return True