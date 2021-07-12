from flask import Blueprint

userauth = Blueprint('userauth', __name__, url_prefix='/userauth')

@userauth.route('/')
def home():
    return 'Hello World!'
