
/**
 * This sets up a mock application server containing the following modules:
 * <ul> 
 * <li>A mock OAuth 2 server under the endpoint /oauth/token that will respond to any request conforming to 
 * the resource owner password credentials flow, giving username "correct user" and password "correct password", 
 * by issuing an access and a refresh token and to any request conforming to the refresh token flow, 
 * presenting the refresh token "correct refresh token", by issuing an access and a refresh token
 * <li>A mock key series request module under the endpoint /sync/key_request that will respond to any GET request
 * with the values next_key=128 and upper_bound=255. It does not perform authorization checking. 
 * </ul>
 * <p>
 * <b>To ensure reproducible test results, it is imperative that the server is entirely stateless!</b>
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

var credentials = { key: fs.readFileSync("mock-server-key.pem", "utf8"), cert: fs.readFileSync("mock-server-cert.pem","utf8") };

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
	if (refreshToken === "correct refresh token" /*|| refreshToken === "refreshtoken"*/) {
		done(null, access, refresh);
	} else {
		// Will give invalid_grant error
		done(null);
	}
}));

app.post('/oauth/token', [ server.token(), server.errorHandler() ]);

/**
 * Create key series request endpoint
 */

app.get("/sync/key_request", function(req,res) {
	res.json({ next_key: 128, upper_bound: 255});
});

https.createServer(credentials, app).listen(app.get('port'), function(){
  console.log('Mock server listening on port ' + app.get('port'));
});
