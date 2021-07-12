from flask import request, jsonify, make_response, Blueprint, current_app
import uuid
from werkzeug.security import check_password_hash
import jwt
import datetime
from functools import wraps
from flask_httpauth import HTTPTokenAuth
from twilio.rest import Client
from sinchsms import SinchSMS
from kavenegar import *
from modules.models import User, db

############################################################################################################################################################

phauth = Blueprint('phauth', __name__, url_prefix='/phauth')

#twilio.com/console
Twilio_account_sid = 'ACad3abe4cdc98b284e014f68b2d18776e'
Twilio_auth_token = '48288e520f3ff135a69f673d47a1cfe4'
Twilio_service = 'VA7e543fbfc4dfc5da01881bf995b981e1'
Twilio_Client = Client(Twilio_account_sid, Twilio_auth_token)

#twilio.com/console
Sinch_app_key = '05490e37-0df5-47bb-bc9a-bcc99313458e'
Sinch_app_secret = 'NzG0XBcZJUK6KEQitA7lbQ=='
Sinch_Client = SinchSMS(Sinch_app_key, Sinch_app_secret)

API_KEY = 'kdD345dKsfJSd3DFU6W235dRWT2d34EFGJ3453HSAdJ23HdfgD'

signup_auth = HTTPTokenAuth(scheme='Bearer')
bearer_auth = HTTPTokenAuth(scheme='Bearer')

#oauth = OAuth(app)

############################################################################################################################################################


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


@phauth.route('/check')
def check():
    return jsonify({'error': False, 'message': "phauth"})


@phauth.route('/signup_request', methods=['POST'])
def signup_request():
    if 'request_key' in request.headers:
        header = jwt.decode(request.headers['request-key'], API_KEY)
        if header['type'] == "secure-registration-request":
            onetime_registration_token = jwt.encode({'type': "registration", 'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=1)}, current_app.config['SECRET_KEY'])
            return jsonify({'registration-access-token': onetime_registration_token.decode('UTF-8')})


@signup_auth.verify_token
def verify_token(registration_access_token):
    auth_token = jwt.decode(registration_access_token, current_app.config['SECRET_KEY'])
    #if data['public_id'] == "registration" and not expired:
    if auth_token['type'] == "registration":
        if 'request_key' in request.headers:
            header = jwt.decode(request.headers['request-key'], API_KEY)
            if header['type'] == "secure-registration-data":
                return header['registration-phone-number']
    elif auth_token['type'] == "validation":
        if 'request_key' in request.headers:
            header = jwt.decode(request.headers['request-key'], API_KEY)
            if header['type'] == "secure-verification-data":
                return {'registration-phone-number': header['registration-phone-number'], 'registration-verifcode': header['registration-verifcode']}
    elif auth_token['type'] == "loginpassword":
        if 'request_key' in request.headers:
            header = jwt.decode(request.headers['request-key'], API_KEY)
            if header['type'] == "secure-password1-data":
                return {'login-phone-number': header['login-phone-number'], 'login-password': header['login-password']}


@phauth.route('/register', methods=['POST'])
@signup_auth.login_required
def register():
    phonenumber = signup_auth.current_user()
    user = User.query.filter_by(phone_number=phonenumber).first()
    SendVerificationSMS(phonenumber)
    onetime_validation_token = jwt.encode({'type': "validation", 'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=1)}, current_app.config['SECRET_KEY'])
    if not user:
        return jsonify({'error': False, 'query': 'signup', 'validation-access-token': onetime_validation_token.decode('UTF-8')})
    else:
        return jsonify({'error': False, 'query': 'login', 'validation-access-token': onetime_validation_token.decode('UTF-8')})


@phauth.route('/verifying', methods=['POST'])
@signup_auth.login_required
def verifying():
    phonenumber_verifcode = signup_auth.current_user()
    phonenumber = phonenumber_verifcode['registration-phone-number']
    verifcode = phonenumber_verifcode['registration-verifcode']
    if ValidateVerificationSMS(phonenumber, verifcode):
        return login(phonenumber)
    else:
        return jsonify(error=True, message='Could not verify')


@phauth.route('/loginpass', methods=['POST'])
@signup_auth.login_required
def loginpass():
    phonenumber_pass = signup_auth.current_user()
    phonenumber = phonenumber_pass['login-phone-number']
    user = User.query.filter_by(phone_number=phonenumber).first()
    if not user:
        return jsonify({'error': True, 'message': 'Could not verify'})
    pass1 = phonenumber_pass['login-password1']
    if check_password_hash(user.private_key, pass1):
        token = jwt.encode({'public_id': user.public_id, 'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=30)}, current_app.config['SECRET_KEY'])
        return jsonify({'error': False, 'message': 'Logged in successfully', 'public_id': user.public_id, 'token': token.decode('UTF-8')})
    else:
        if check_password_hash(user.private_key_hist, pass1):
            return jsonify(error=True, message='You have changed your password!')
        return jsonify(error=True, message='Could not verify')


def SendVerificationSMS(phonenumber):
    #return TwilioSMS(phonenumber)
    return True


def ValidateVerificationSMS(phonenumber, verifcode):
    #return TwilioVerify(phonenumber, verifcode)
    if verifcode == '12345':
        return True
    else:
        return False


def TwilioSMS(phonenumber):
    verification = Twilio_Client.verify.services(Twilio_service).verifications.create(to=phonenumber, channel='sms')
    print(verification.status)
    return make_response(verification.status, 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})


def TwilioVerify(phonenumber, verifcode):
    verification_check = Twilio_Client.verify \
                           .services(Twilio_service) \
                           .verification_checks \
                           .create(to=phonenumber, code=verifcode)
    print(verification_check.status)
    return make_response(verification_check.status, 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})


def login(phonenumber):
    user = User.query.filter_by(phone_number=phonenumber).first()
    if not user:
        new_user = User(
            public_id=str(uuid.uuid4()),
            public_key=phonenumber,
            phone_number=phonenumber,
            email=phonenumber,
            building_id='',
            private_key='',
            private_key_hist='',
            private_key2='',
            private_key2_hist='',
            devices='',
            public_name=phonenumber,
            privacy=000,
            permissions=000
        )

        db.session.add(new_user)
        db.session.commit()

    this_user = User.query.filter_by(phone_number=phonenumber).first()

    if this_user.private_key != '':
        onetime_validation_token = jwt.encode({'type': 'pass_validation', 'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=1)}, current_app.config['SECRET_KEY'])
        return jsonify({'error': False, 'message': 'Password is needed', 'query': 'pass1', 'pass1-access-token': onetime_validation_token.decode('UTF-8')})

    token = jwt.encode({'public_id': this_user.public_id, 'exp': datetime.datetime.utcnow() + datetime.timedelta(minutes=30)}, current_app.config['SECRET_KEY'])
    return jsonify({'error': False, 'message': 'Logged in successfully', 'query': '', 'public_id': this_user.public_id, 'token': token.decode('UTF-8')})

############################################################################################################################################################


@phauth.route('/otp')
def TestSMS():
    try:
        api = KavenegarAPI('Your APIKey', timeout=20)
        params = {
            'receptor': '+989124101723',
            'template': '',
            'token': 'B2343sd',
            'type': 'sms',#sms or call
        }
        response = api.verify_lookup(params)
        print(response)
    except APIException as e:
        print(e)
    except HTTPException as e:
        print(e)

@phauth.route('/validation')
@signup_auth.login_required
def Testvalidation():
    phonenumber = signup_auth.current_user()
    user = User.query.filter_by(phone_number=phonenumber).first()
    if not user:
        new_user = User(public_id=str(uuid.uuid4()), public_key=phonenumber, phone_number=phonenumber, email="sdas@sds.c", private_key='', public_name=phonenumber, privacy=222, permissions=777)
        db.session.add(new_user)
        db.session.commit()
        this_user = User.query.filter_by(phone_number=phonenumber).first()
        return jsonify(error=False, message='User registered successfully', id=this_user.id)
    else:
        return make_response('This username has been taken.', 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})


@phauth.route('/TwilioSMS')
def TestTwilioSMS():
    verification = Twilio_Client.verify.services(Twilio_service).verifications.create(to='+989124101723', channel='sms')
    print(verification.status)
    return make_response(verification.status, 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})

@phauth.route('/SinchSMS')
def TestSinchSMS():
    response = Sinch_Client.send_message("+989124101723", "Cb264Ba76")
    return make_response("sent", 400, {'WWW-Authenticate': 'Basic realm="Login required!"'})

@phauth.route('/TwilioVerify', methods=['POST'])
def TestTwilioVerify():
    data = request.get_json()
    verification_check = Twilio_Client.verify \
                           .services(Twilio_service) \
                           .verification_checks \
                           .create(to='+989124101723', code=data['code'])
    print(verification_check.status)
    return make_response(verification_check.status, 401, {'WWW-Authenticate': 'Basic realm="Login required!"'})


############################################################################################################################################################


