# ShoppingSystem

## Configurations

The System has 4 different Configuration Profiles, which are listed 
under the project/src/main/resources/ folder.
Each '*.properties' file represents a singular configuration profile.

the main ones are: 

##### Base Profile - application.properties

this file holds the base data that every other profile inherits.
And is the base default profile to run the system with.
Includes DB.

How to run: mvn spring-boot:run

##### No DB Profile - application-no-db.properties

this file inherits all of the data from the base profile, but runs
the system with no connection to the database. everything is 'in-memory'.
therefore none of the JPA Repositories will be used, and instead the regular
in-memory ones will.

How to run: mvn spring-boot:run '-Dspring-boot.run.profiles=no-db'

## Intialization

when running with a db inclusive profile, the project/src/main/resources/import.sql 
file is ran, which consists of a sql script to initialize the database with some entities.

the entities are:

##### Users (Members):

* Six users are added: u1 through u6.
* u1 is the lone administrator (is_admin = TRUE).
* All others (u2 – u6) are regular members.
* Every member starts offline (is_connected = FALSE).
* payment_method_string is NULL for all six members.

##### Authentication:

* A single login token is generated:
    * token-u2 belongs to u2.
    * It expires exactly one hour after the insertion time (DATEADD('HOUR', 1, CURRENT_TIMESTAMP)).

##### Shop and Inventory:
* One shop is created: s1 (shop ID 1) and is open.
    * WSEPShipping is set as its shipping method.

* One product is introduced: Bamba (item ID 1).
    * Description: Crunchy peanut snack.
    * Quantity: 20 units.
    * Price: 30.


##### Role Delegations

All role assignments originate from u2 and apply to shop 1:

* u3 receives manageItems (0) and leaveShopAsManager (4).
* u4 and u5 each receive leaveShopAsManager (4) only.
