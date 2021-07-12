from flask_sqlalchemy import SQLAlchemy
import time
from authlib.integrations.sqla_oauth2 import (
    OAuth2ClientMixin,
    OAuth2AuthorizationCodeMixin,
    OAuth2TokenMixin,
)

db = SQLAlchemy()


class User(db.Model):
    __tablename__ = "user"
    id = db.Column(db.Integer, primary_key=True)
    public_id = db.Column(db.String(50), unique=True)
    public_key = db.Column(db.String(50), unique=True)
    phone_number = db.Column(db.String(50), unique=True)
    email = db.Column(db.String(50), unique=True)
    building_id = db.Column(db.String(50))
    apartment_id = db.Column(db.String(50))
    private_key = db.Column(db.String(200))
    private_key_hist = db.Column(db.String(200))
    private_key2 = db.Column(db.String(200))
    private_key2_hist = db.Column(db.String(200))
    devices = db.Column(db.String(2000))
    public_name = db.Column(db.String(50))
    privacy = db.Column(db.Integer)
    permissions = db.Column(db.Integer)


class Building(db.Model):
    __tablename__ = "building"
    id = db.Column(db.Integer, primary_key=True)
    building_id = db.Column(db.String(50), unique=True)
    building_name = db.Column(db.String(50))
    charge_per_month = db.Column(db.Integer)
    manager_phonenumber = db.Column(db.String(20))


class Apartment(db.Model):
    __tablename__ = "apartment"
    id = db.Column(db.Integer, primary_key=True)
    apartment_id = db.Column(db.String(50), unique=True)
    apartment_name = db.Column(db.String(50))
    building_id = db.Column(db.String(50))
    fee_per_month = db.Column(db.Integer)
    lodger_phonenumber = db.Column(db.String(20))
    owner_phonenumber = db.Column(db.String(20))


class Caretaker(db.Model):
    __tablename__ = "caretaker"
    id = db.Column(db.Integer, primary_key=True)
    caretaker_id = db.Column(db.String(50), unique=True)
    caretaker_name = db.Column(db.String(50))
    building_id = db.Column(db.String(50))
    caretaker_phonenumber = db.Column(db.String(20))
    payment_account = db.Column(db.String(200))
    cost = db.Column(db.Integer)


class Transaction(db.Model):
    __tablename__ = "transaction"
    id = db.Column(db.Integer, primary_key=True)
    transaction_id = db.Column(db.String(50), unique=True)
    user_public_id = db.Column(db.String(50))
    type = db.Column(db.String(50))
    info = db.Column(db.String(500))
    time_created = db.Column(db.Boolean)
    time_deadline = db.Column(db.Boolean)
    time_payed = db.Column(db.Boolean)
    ispayed = db.Column(db.Boolean)
    apartment_id = db.Column(db.String(50))
    transcript_ids = db.Column(db.String(250))


class Todo(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    text = db.Column(db.String(50))
    complete = db.Column(db.Boolean)
    user_id = db.Column(db.Integer)

