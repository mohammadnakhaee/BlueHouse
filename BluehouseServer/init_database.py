#For sqlite
from api import db
db.create_all()

#For mysql
#from app import User
#user = User.query.all()
#print(user)

#remove the connection error
#see here
#https://www.percona.com/blog/2019/04/18/mysql-python-adding-caching_sha2_password-tlsv1-2-support/
#Authentication plugin 'caching_sha2_password' cannot be loaded
#1) open mysql shell
#2) if it is on JS mode change it to sql using
#>>\sql
#3)connect to local host
#>>\connect root@localhost
#4)run the following command
#>>ALTER USER root@localhost IDENTIFIED WITH mysql_native_password BY 'q1w2e3r4t5y6';

#this method is only for localhost only. It should be check how to connect securely
#see here
#https://www.percona.com/blog/2019/04/18/mysql-python-adding-caching_sha2_password-tlsv1-2-support/
#or
#the MySQL-python_ Adding caching_sha2_passw.pdf file in this directory:
