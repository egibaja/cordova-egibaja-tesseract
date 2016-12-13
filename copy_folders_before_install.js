var path = require("path");
var rootdir = __dirname;
console.log(rootdir);

console.log("Running hook: " + path.basename(__filename));

var fs = require('fs.extra');
 
var lib_source = path.join(rootdir,'libs');
var lib_dest = path.join(rootdir,'..', '..', 'platforms', 'android', 'libs');

console.log(lib_dest)

fs.copyRecursive(lib_source, lib_dest, function (err) {
 if (err) {
   return console.error(err);
 }
 console.log('lib done!');
});

var tessdata_source = path.join(rootdir,'tessdata');
var tessdata_dest = path.join(rootdir,'..', '..', 'platforms', 'android', 'assets');
console.log("Tessdata source and dest")
console.log(tessdata_source)
console.log(tessdata_dest)

 
fs.copyRecursive(tessdata_source, tessdata_dest, function (err) {
 if (err) {
   return console.error(err);
 }
 console.log('tessdata done!');
});