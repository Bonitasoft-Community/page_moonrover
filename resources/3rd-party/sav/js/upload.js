'use strict';
/**
 * 
 */

(function() {

	var appCommand = angular.module('fileUpload', ['angularFileUpload']);

//	Constant used to specify resource base path (facilitates integration into a Bonita custom page)
	appCommand.constant('RESOURCE_PATH', 'pageResource?page=custompage_bardigester&location=');

	appCommand.controller('UploadFileController', function($scope, $upload) {
		$scope.$watch('files', function() {
			for (var i = 0; i < $scope.files.length; i++) {
				var file = $scope.files[i];
				$scope.upload = $upload.upload({
					method: 'POST',
					url: '?page=custompage_bardigester&action=uploadbar', // upload.php script, node.js route, or servlet url
					//headers: {'Authorization': 'xxx'}, // only for html5
					//withCredentials: true,
					data: {myObj: $scope.myModelObj},
					file: file, // single file or a list of files. list is only for html5
					//fileName: 'doc.jpg' or ['1.jpg', '2.jpg', ...] // to modify the name of the file(s)        //fileFormDataName: myFile, // file formData name ('Content-Disposition'), server side request form name
					// could be a list of names for multiple files (html5). Default is 'file'
					//formDataAppender: function(formData, key, val){}  // customize how data is added to the formData. 
					// See #40#issuecomment-28612000 for sample code

				}).progress(function(evt) {
					console.log('progress: ' + parseInt(100.0 * evt.loaded / evt.total) + '% file :'+ evt.config.file.name);
				}).success(function(data, status, headers, config) {
					// file is uploaded successfully
					console.log('file ' + config.file.name + 'is uploaded successfully. Response: ' + data);
				});
				//.error(...)
				//.then(success, error, progress); // returns a promise that does NOT have progress/abort/xhr functions
				//.xhr(function(xhr){xhr.upload.addEventListener(...)}) // access or attach event listeners to 
				//the underlying XMLHttpRequest
			}
			/* alternative way of uploading, send the file binary with the file's content-type.
		       Could be used to upload files to CouchDB, imgur, etc... html5 FileReader is needed. 
		       It could also be used to monitor the progress of a normal http post/put request. 
		       Note that the whole file will be loaded in browser first so large files could crash the browser.
		       You should verify the file size before uploading with $upload.http().
			 */
			// $scope.upload = $upload.http({...})  // See 88#issuecomment-31366487 for sample code.

		});
	});
})();
