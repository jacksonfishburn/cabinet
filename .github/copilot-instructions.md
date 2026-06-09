I want to refactor the front end to work with the new API.
The new API has a different structure and different endpoints than the old API.
I want to make sure that the front end is still able to communicate
with the back end and that all of the functionality is still working properly.
I also want to make sure that the front end is still able to handle any errors
that may occur when communicating with the back end.


ive added shared cabinets feature to the server, I do not need the React front 
end to work with the shared cabinets feature, but I do want for the endpoints that 
the front end currently uses to still work properly. This will require changes
starting from api.ts and then to the components that use the api as well as anything 
else you might see fit. the login and register should still work just fine.
