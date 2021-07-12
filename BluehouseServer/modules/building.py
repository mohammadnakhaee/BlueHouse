from flask import request, jsonify, make_response, Blueprint, current_app
import uuid
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
import datetime
from functools import wraps
from modules.models import User, Apartment, db, Building

############################################################################################################################################################


building = Blueprint('building', __name__, url_prefix='/building')

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


@building.route('/check')
def check():
    return jsonify({'error': False, 'message': "building"})


@building.route('/find_by_manager')
@token_required
def find_by_manager(current_user):
    data = request.get_json()
    building_array = Building.query.filter_by(manager_phonenumber=data["manager_phonenumber"]).all()

    output = []
    for b in building_array:
        data = {}
        data['building_id'] = b.building_id
        data['building_name'] = b.building_name
        data['charge_per_month'] = b.charge_per_month
        data['manager_phonenumber'] = b.manager_phonenumber
        output.append(data)

    return jsonify({'error': False, 'buildings': output})


@building.route('/add_building', methods=['POST'])
@token_required
def add_building(current_user):
    data = request.get_json()

    new_building = Building(
        building_id=str(uuid.uuid4()),
        building_name=data["building_name"],
        charge_per_month=0,
        manager_phonenumber=data["manager_phonenumber"]
    )

    try:
        db.session.add(new_building)
        db.session.commit()
    except:
        return jsonify({'error': True, 'message': 'Unable to add building.'}), 401

    current_user.building_id = new_building.building_id
    return jsonify({'error': False, 'message': "Building is added."})


@building.route('/my_buildings')
@token_required
def my_buildings(current_user):
    building_array = Building.query.filter_by(building_id=current_user.building_id).all()

    output = []
    for b in building_array:
        data = {}
        data['building_id'] = b.building_id
        data['building_name'] = b.building_name
        data['charge_per_month'] = b.charge_per_month
        data['manager_phonenumber'] = b.manager_phonenumber
        output.append(data)

    return jsonify({'error': False, 'buildings': output})


@building.route('/set_building_charge', methods=['POST'])
@token_required
def set_building_charge(current_user):
    data = request.get_json()
    building = Building.query.filter_by(building_id=data['building_id'], manager_phonenumber=current_user.phone_number).first()

    if not building:
        return jsonify({'error': True, 'message': 'Permission denied.'}), 401

    building.charge_per_month = data['charge_per_month']
    return jsonify({'error': False, 'message': 'Changed successfully.'})


############################################################################################################################################################
