from modules.app import create_app

#app.config['SECRET_KEY'] = 'thisissecret'
#app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:////mnt/c/Users/antho/Documents/api_example/todo.db'

#app.config['SQLALCHEMY_POOL_RECYCLE'] = 280
#app.config['SQLALCHEMY_POOL_SIZE'] = 20
#'OAUTH2_REFRESH_TOKEN_GENERATOR': True,

app = create_app({
    'SECRET_KEY': 'bluehousesecretcode!',
    'SQLALCHEMY_TRACK_MODIFICATIONS': False,
    'SQLALCHEMY_DATABASE_URI': 'mysql://root:q1w2e3r4t5y6@localhost/bluehouseaccounts',
    'AUTHLIB_INSECURE_TRANSPORT': True,
})


#if __name__ == '__main__':
#    app.run(debug=True)

if __name__ == '__main__':
    app.run(host='0.0.0.0', debug=True)
