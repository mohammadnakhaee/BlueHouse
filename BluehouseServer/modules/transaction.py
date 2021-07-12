from flask import request, jsonify, make_response, Blueprint, current_app
import uuid
from werkzeug.security import generate_password_hash, check_password_hash
import jwt
import datetime
from functools import wraps
from modules.models import User, Transaction, db

############################################################################################################################################################


transaction = Blueprint('transaction', __name__, url_prefix='/transaction')

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


@transaction.route('/check')
def check():
    return jsonify({'error': False, 'message': "transaction"})


@transaction.route('/getall', methods=['GET'])
@token_required
def getall(current_user):
    trans = Transaction.query.filter_by(user_public_id=current_user.public_id).all()

    output = []

    for t in trans:
        data = {}
        data['type'] = t.type
        data['info'] = t.info
        data['time_created'] = t.time_created
        data['time_deadline'] = t.time_deadline
        data['time_payed'] = t.time_payed
        data['ispayed'] = t.ispayed
        data['apartment_id'] = t.apartment_id
        data['transcript_ids'] = t.transcript_ids
        output.append(data)

    return jsonify({'transactions': output})


'''
def create_trans():
    data = request.get_json()

    new_trans = Transaction(
        transaction_id = ,
        user_public_id = ,
        type = ,
        info = ,
        time_created = ,
        time_deadline = ,
        time_payed = ,
        apartment_id = ,
        transcript_ids = data['text'],
    )

    db.session.add(new_trans)
    db.session.commit()

    return jsonify({'message' : "Todo created!"})
'''

############################################################################################################################################################
