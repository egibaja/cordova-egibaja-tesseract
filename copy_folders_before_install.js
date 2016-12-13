var fs = require("fs");
var path = require("path");
var rootdir = __dirname;
console.log(rootdir);

console.log("Running hook: " + path.basename(__filename));

var ncp = require('ncp').ncp;

ncp.limit = 16;
 
var lib_source = path.join(rootdir,'libs');
var lib_dest = path.join(rootdir,'..', '..', 'platforms', 'android', 'libs')

console.log(lib_dest)

ncp(lib_source, lib_dest, function (err) {
 if (err) {
   return console.error(err);
 }
 console.log('lib done!');
});

var tessdata_source = path.join(rootdir,'tessdata');
var tessdata_dest = path.join(rootdir,'..', '..', 'platforms', 'android', 'assets')

console.log(tessdata_dest)

 
ncp(tessdata_source, tessdata_dest, function (err) {
 if (err) {
   return console.error(err);
 }
 console.log('tessdata done!');
});