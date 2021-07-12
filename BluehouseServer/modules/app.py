import os
from flask import Flask
from modules.phauth import phauth
from modules.transaction import transaction
from modules.models import db

#import logging
#import sys
#log = logging.getLogger('authlib')
#log.addHandler(logging.StreamHandler(sys.stdout))
#log.setLevel(logging.DEBUG)


############################################################################################################################################################


def create_app(config=None):
    app = Flask(__name__)

    # load default configuration
    #app.config.from_object('module.settings')

    # load environment configuration
    if 'MODULE_CONF' in os.environ:
        app.config.from_envvar('MODULE_CONF')

    # load app specified configuration
    if config is not None:
        if isinstance(config, dict):
            app.config.update(config)
        elif config.endswith('.py'):
            app.config.from_pyfile(config)

    setup_app(app)
    return app


def setup_app(app):
    # Create tables if they do not exist already
    @app.before_first_request
    def create_tables():
        db.create_all()

    db.init_app(app)
    #config_oauth(app)
    #app.register_blueprint(userauth, url_prefix='')
    app.register_blueprint(phauth)
    app.register_blueprint(transaction)




