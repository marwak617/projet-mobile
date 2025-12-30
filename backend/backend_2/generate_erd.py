from eralchemy import render_er
from database import Base  # ton Base SQLAlchemy

# Génère le diagramme ERD en image
render_er(Base, "schema_erd.png")

# Option PDF
render_er(Base, "schema_erd.pdf")
