# Privat24 statements export to Manager.io API

Web application for Privat24 for business statements import and Manager.io API export


Works with [Privat24 API](https://link.privatbank.ua/console/wiki)

and locally installed [Manager](https://www.manager.io) server

## Usage

    $ lein ring server

    or
    
    $ lein run itservice
    
    this loads "itservice" configuration and starts web server on localhost port 8080
    
   

    how build and run in production

    $ lein uberjar 
    
    $ java -jar target/uberjar/privat-manager-1.0.1-standalone.jar

and open browser http://localhost:3000/statements


![Screenshot 1](/doc/shot1.png?raw=true "Screenshot 1")


![Screenshot 2](/doc/shot2.png?raw=true "Screenshot 2")


![Screenshot 3](/doc/shot3.png?raw=true "Screenshot 3")


![Screenshot 4](/doc/shot4.png?raw=true "Screenshot 4")


![Screenshot 5](/doc/shot5.png?raw=true "Screenshot 5")
## License

Copyright © 2017 Maksym Prokopov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
