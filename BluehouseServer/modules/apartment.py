from flask import request, jsonify, make_response, Blueprint, current_app
import uuid
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
import datetime
from functools import wraps
from modules.models import User, Apartment, db

############################################################################################################################################################


apartment = Blueprint('apartment', __name__, url_prefix='/apartment')

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None

        if 'x-access-token' in request.headers:
            token = request.headers['x-access-token']

        if not token:
            return jsonify({'message': 'Token is missing!'}), 401

        try:
            data = jwt.decode(token, current_app.config['SECRET_KEY'])
            current_user = User.query.filter_by(public_id=data['public_id']).first()
        except:
            return jsonify({'message': 'Token is invalid!'}), 401

        return f(current_user, *args, **kwargs)

    return decorated


############################################################################################################################################################


@apartment.route('/check')
def check():
    return jsonify({'error': False, 'message': "apartment"})


@apartment.route('/add_apartment', methods=['POST'])
@token_required
def add_apartment(current_user):
    data = request.get_json()

    new_apartment = Apartment(
        apartment_id=str(uuid.uuid4()),
        building_id="",
        fee_per_month=0,
        lodger_phonenumber=current_user.phone_number,
        owner_phonenumber=data["owner_phonenumber"],
    )

    db.session.add(new_apartment)
    db.session.commit()
    return jsonify({'error': False, 'message': "Apartment created."})


@apartment.route('/my_apartments')
@token_required
def getall(current_user):
    apart = Apartment.query.filter_by(lodger_phonenumber=current_user.phone_number).all()

    output = []

    for t in apart:
        data = {}
        data['apartment_id'] = t.apartment_id
        data['building_id'] = t.building_id
        data['fee_per_month'] = t.fee_per_month
        data['lodger_phonenumber'] = t.lodger_phonenumber
        data['owner_phonenumber'] = t.owner_phonenumber
        output.append(data)

    return jsonify({'error': False, 'apartment': output})


############################################################################################################################################################
