
/**
 * This sets up a mock OAuth 2 server under the endpoint /oauth/token that will
 * <ul>
 * <li>respond to any request conforming to the resource owner password credentials flow, giving
 * username "correct user" and password "correct password", by issuing an access and a refresh token
 * <li>respond to any request conforming to the refresh token flow, presenting the refresh token
 * "correct refresh token", by issuing an access and a refresh token
 * </ul>
 */
var express = require('express');
var fs = require("fs");
var https = require('https');
var path = require('path');
var oauth2orize = require("oauth2orize");
var app = express();

// all environments
app.set('port', process.env.PORT || 3000);
app.use(express.logger('dev'));
app.use(express.json());
app.use(express.urlencoded());
app.use(express.methodOverride());
app.use(express.cookieParser("Mock secret"));
app.use(express.session());
app.use(app.router);

var credentials = { key: fs.readFileSync("mock-auth-server-key.pem", "utf8"), cert: fs.readFileSync("mock-auth-server-cert.pem","utf8") };

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

/*
 * Create oauth 2 server
 */ 
var server = oauth2orize.createServer();
server.exchange(oauth2orize.exchange.password(function(client, username, password, scope, done) {
	var access = "accesstoken";
	var refresh = "refreshtoken";
	
	if (username === "correct user" && password === "correct password") {
		lastRefresh = refresh;
		done(null, access, refresh);
	} else {
		// Will give invalid_grant error
		done(null);
	}
}));
server.exchange(oauth2orize.exchange.refreshToken(function(client, refreshToken, scope, done) {
	var access = "newaccesstoken";
	var refresh = "refreshtoken";
	if (refreshToken === "correct refresh token") {
		done(null, access, refresh);
	} else {
		// Will give invalid_grant error
		done(null);
	}
}));

app.post('/oauth/token', [ server.token(), server.errorHandler() ]);


https.createServer(credentials, app).listen(app.get('port'), function(){
  console.log('Mock OAuth 2.0 server listening on port ' + app.get('port'));
});
