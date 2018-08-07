'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('moonrovermonitor', ['googlechart', 'ui.bootstrap','ngSanitize', 'ngModal', 'angularFileUpload']);

// Constant used to specify resource base path (facilitates integration into a Bonita custom page)





// --------------------------------------------------------------------------
//
// Controler
//
// --------------------------------------------------------------------------

// User app list controller
appCommand.controller('moonroverController',
	function ( $http, $scope, $sce, $interval,$filter ) {

	this.inprogress=false;
	this.isinitialised=false;
	
	// -----------------------------------------------------------------------------------------
	//  										Data
	// -----------------------------------------------------------------------------------------
	this.simulation= {'listsources': [ 
							{'name':'Event Ste (BDM)', 'value' : 'BDM:Eventste', 'type':'BDM'}, 
							{'name' : 'Evenement STE (Process)', 'value': 'PROCESS:Evenement ste', 'type':'PROC'}
							],
						'listselections': [ 
							{'name': 'standard', 'value': 'standard', 'type': 'STD'},
							{'name': 'Direct sql', 'value': 'Direct', 'type': 'SQL'},
							{'name': 'findByNumSemaine', 'value': 'findByNumSemaine', 'type':'FIND'},
							{'name': 'findByQuart', 'value': 'findByQuart', 'type':'FIND'},
							{'name': 'findByTranche', 'value': 'findByTranche', 'type':'FIND'}
							],
						'listfields': [ 
							{'name': 'Tranche', 'value': '', 'type': 'TEXT', 'length':30},
							{'name': 'Quart', 'value': '', 'type': 'TEXT', 'length':30},
							]
					};
	
	this.definition = {	'listsources':[],
						'listselections':[]
						
					}; 
	this.request= { 'sourcename' : '',
				'source':{}, 
				'selection': {'parametersvalue':[]}, 
				'order' : []				
			};
	this.lists = {
			"listoperators" : [
				 { "name": "", 	   "value":""},
				 { "name": "Egal", "value":"EQUALS"},
				 { "name": "Null", "value":"ISNULL"},
				 { "name": "Diff", "value":"DIFF"},
				 { "name": "Like", "value":"LIKE"},
				 { "name": "Upper", "value":"UPPER"},
				 { "name": "Lower", "value":"LOWER"},
				 { "name": "From", "value":"RANGE"}
								],
				"listboolean" : [ 
					{ "name": "", 	   "value":null},
					{ "name": "True", "value":"true"},
					{ "name": "False", "value":"false"}
				],
				"listorderdirection" :[ 
					{ "name": "Ascendant", 	"value":"ASC"},
					{ "name": "Descendant", "value":"DESC"}
				],
				"listtyperesult": [ 
					{ "name": "Editable Record", "value":"EDITRECORD"},
					{ "name": "Table", 	   	 "value":"TABLE"}
				 // { "name": "Chart", 	   "value":"CHART"},
				 // { "name": "Jasper", 	   "value":"JASPER"}
				],
				"listTypesField": [
					 { "name": "String", 	   "value":"STRING"},
					 { "name": "Numeric", 	   "value":"NUM"}
					 // DATE, BOOLEAN
					
				]
			};
	
	
	// -----------------------------------------------------------------------------------------
	//  										Source
	// -----------------------------------------------------------------------------------------
	this.loadSources = function()
	{
		var self=this;
		
		self.inprogress=true;
		//alert("source= "+ angular.toJson(self.simulation.listsources, true))
		//self.request.listsources = self.simulation.listsources; 
		console.log("noonrover.loadSources");
		var param={};
		var json= encodeURIComponent( angular.toJson( param, true));
		var d = new Date();
		
		var url='?page=custompage_moonrover&action=loadsources&paramjson='+json+'&t='+d.getTime();
		$http.get( url )
				.success( function ( jsonResult ) {
							self.inprogress						= false;
							self.definition.listevents = jsonResult.listevents;
							self.definition.listsources = jsonResult.listsources;
							self.definition.listselections=[];


							console.log("noonrover.loadSources : receive list");
							}
				)
				.error( function ( result ) {
							self.inprogress						= false;
							
							}

				);
	}
	this.getListSources = function(  )
	{
		return this.definition.listsources;
		
	};

	this.setSource = function()
	{		
		var source = null;
		var sourcename=this.request.sourcename;
		console.log("noonrover.setSource name=["+sourcename+"]");
	
		for (var i in this.definition.listsources)
		{
			var sourceI = this.definition.listsources[ i ];
			if (sourceI.sourcename === sourcename)
				source = sourceI;
		}
		if (! source)
		{
			console.log("noonrover.noSourceFound ["+sourcename+"]");
			return;
		}		
		// setup the direct access then it's more simple for the next part
		this.request.source=source;
		this.definition.listselections = source.selections;
		
		// prepare the first selection to be symphatical to the user

		if (this.definition.listselections.length > 0 )
		{
			// console.log("noonrover.setSource selection0="+angular.toJson(this.definition.listselections[0]));
			
			this.request.selection = this.definition.listselections[0];
			this.request.selection_name=this.request.selection.name;
			this.request.selection.showAll=true;
			
			this.setSelection();
		}
		// alert("Check type request: "+angular.toJson(this.request));
		this.request.result = this.request.source.result;
		this.request.result.showAll=true;
		console.log("noonrover.setSource request.result="+angular.toJson(this.request.result));
		
		if ( (! this.request.result.hasOwnProperty('typeresult')) ||  (this.request.result.typeresult=== null) )
		{
			console.log("noonrover.setSource set a default typeresult");
			this.request.result.typeresult = this.lists.listtyperesult[ 0 ].value; 
		}
	}
	
	//------------------ selection
	this.getListSelection = function( )
	{
		// console.log("noonrover.getListSelection ["+this.definition.listselections+"]");	
		// According the source selected, get all available selection
		if (this.definition.listselections!==null)
		{
			return this.definition.listselections;
		}
		// if we just load the result, then this is the list
		var listSelection = [ { 'name':this.request.selection.name}];
		return listSelection;
	}
	this.setSelection = function ()
	{
		// this.request.listselectionfields =  this.simulation.listfields
		console.log("noonrover.setSelection ["+this.request.selection_name+"]");
		var selection = null;
		var selectionname=this.request.selection_name;
		for (var i in this.definition.listselections)
		{
			var selectionI = this.definition.listselections[ i ];
			if (selectionI.name === selectionname)
				selection = selectionI;
		}
		if (! selection)
			return;
		
		// setup the direct access then it's more simple for the next part
		this.request.selection=selection;
		this.request.listselectionfields = selection.selections;

	}
	
	
	this.isSelectionFieldVisible = function ( fieldParameter ) {
		if ( this.request.selection.showAll)
			return true;
		
		if (fieldParameter.visible)
			return true;
		return false;
		/*
		if (fieldParameter.operator === null)
			return '3-false';
		if (fieldParameter.operator === "")
			return '4-false';
		return '5-true [['+fieldParameter.operator+"]]";
		*/
	}
	
	// -------------------- According the selection, get the parameters 
	this.getListParameters =function(){
		return this.request.selection.parameters;
	}
	
	// this is the sum of the parameters AND parametersvalue
	this.getListFormParameters = function() {
		if (this.request.selection.type==='SQL')
			return this.request.selection.parametersvalue;		
		else
			return this.request.selection.parameters;
	}
	
	this.getListOperator = function( fieldParameters )
	{
		return this.lists.listoperators;
	}
	this.getListTypesField = function( fieldParameters )
	{
		return this.lists.listTypesField;
	}
	
	this.getListBoolean = function( fieldParameters )
	{
		return this.lists.listboolean;
	}
	this.selectAllParameters = function( visible, list )
	{
		for(var i in list)
		{
			var parameter = list[ i ];
			parameter.visible = visible;
		}
	}
	
	// -- Different type of result
	

	this.getListTypeResult= function()
	{ 
		if (this.lists)
			return this.lists.listtyperesult;
		return {};
	}
	// --------------------- result : column
	this.getListColumnsResult  =function()
	{
		if (this.request.result)
			return this.request.result.columns;
		return {};
	}
	
	this.getRequest = function()
	{
		var request= { 	'sourcename': this.request.sourcename, 
			'selection': this.request.selection,
			'result' : this.request.result,
			'order' : this.request.order,
			'startindex': this.display.numberperpage.value * (this.display.pagenumber-1),
			'maxresults': this.display.numberperpage.value
		}
		return request;
	}
	// -----------------------------------------------------------------------------------------
	//  										Save / Load Request
	// -----------------------------------------------------------------------------------------
	this.saveinfo = { 'name':'', 'description':'', 'listevents': '', 'listrequests':[ ]};
	// Save
	this.saveRequest = function() {
		var param={ 'name' : this.saveinfo.name,
				'description': this.saveinfo.description,
				'request': this.getRequest() };
		this.report.listevents ='';
		
		// console.log("Save=");

		this.bigPost( param, "saveRequest");
	}
	// Load
	this.loadRequest = function( item ) {
		var param={'name' : item.name };
		var json= encodeURIComponent( angular.toJson( param, true));
		var self=this;
		self.saveinfo.name 					= item.name;
		self.saveinfo.description 			= item.description;
		self.inprogress						= true;
		self.report.listevents ='';
		var d = new Date();
		
		var url='?page=custompage_moonrover&action=loadRequest&paramjson='+json+'&t='+d.getTime();
		$http.get( url )
				.success( function ( jsonResult ) {
			
					// update the corresponding selection, and switch on
					self.inprogress						= false;
					self.saveinfo.listevents = jsonResult.listevents;
					self.request = jsonResult.content.request;
					// we don't have the definition part, so clean it
					self.definition.listselections=null;
					self.request.selection=selection;
					self.request.listselectionfields = selection.selections;
					console.log("noonrover.loadSelection : receive ");
					}
				)
				.error( function ( result ) {
							self.inprogress						= false;
							
							}

				);
	}
	this.deleteRequest = function ( name) {
		if (confirm("Would you like to delete this request?"))
		{
			var param={'name' : name };
			var json= encodeURIComponent( angular.toJson( param, true));
			var self=this;
			self.inprogress						= true;
			self.report.listevents ='';
			var d = new Date();
			
			var url='?page=custompage_moonrover&action=deleteRequest&paramjson='+json+'&t='+d.getTime();
			$http.get( url )
			.success( function ( jsonResult ) {
				
						// update the corresponding selection, and switch on
						self.inprogress						= false;
						self.saveinfo.listevents = jsonResult.listevents;
						self.saveinfo.listevents = jsonResult.listevents;
						self.saveinfo.listrequests=jsonResult.listrequests;
			})
			.error( function ( result ) {
						self.inprogress						= false;
						
						}

			);
		}
	}
	
	this.listRequests = function() {
		var param={};
		var json= encodeURIComponent( angular.toJson( param, true));
		var self=this;
		self.inprogress						= true;
		var d = new Date();
		
		var url='?page=custompage_moonrover&action=listRequests&paramjson='+json+'&t='+d.getTime();
		$http.get( url )
				.success( function ( jsonResult ) {
							self.inprogress						= false;
							self.saveinfo.listevents = jsonResult.listevents;
							self.saveinfo.listrequests=jsonResult.listrequests;
							console.log("noonrover.loadListSelection : receive ");
						}
				)
				.error( function ( result ) {
							self.inprogress						= false;
							
							}

				);
	}
	this.getListRequests = function ()
	{
		return this.saveinfo.listrequests;
	}
	// -----------------------------------------------------------------------------------------
	//  										Sql parameters
	// -----------------------------------------------------------------------------------------
	this.getListSqlParameters = function() {
		if (this.request.selection.parametersvalue ==null)
			this.request.selection.parametersvalue=[];
		// recreat the index
		for(var i = this.request.selection.parametersvalue.length - 1; i >= 0; i--) {
			this.request.selection.parametersvalue[ i ].index=i;
		}
		return this.request.selection.parametersvalue;
	}
	this.addSqlParameter = function() {
		
		if (this.request.selection.parametersvalue ==null)
			this.request.selection.parametersvalue=[];
		var value= { 'index':  this.request.selection.parametersvalue.length, 'type': 'STRING' }
		return this.request.selection.parametersvalue.push( value );
	} 
	this.deleteSqlParameter= function( index ) {
    	this.request.selection.parametersvalue.splice(index, 1);
	}
	// -----------------------------------------------------------------------------------------
	//  										order
	// -----------------------------------------------------------------------------------------
	this.getListColumnsOrders = function() {
		return this.request.order;
	}
	this.getListOrderDirection = function( fieldOrder ) {
		return this.lists.listorderdirection;
	}
	this.setOrder = function ( columnid, isAscendant)
	{
		var sens="";
		if (isAscendant)
			sens="ASC";
		else
			sens="DESC";
		this.request.order =[ { "columnid" : columnid,
							"direction" : sens
							} ];
		// run immediately the request
		this.runRequestConfiguration();
		
	}
	this.addColumnsOrder = function()
	{
		var oneCriteria = { "columnid":"", "direction": "ASC"};
		this.request.order.push(oneCriteria);
	}
	
	this.getOrderByField = function () {
		if (this.request.order.length > 0 )
			return this.request.order[0].columnid;
		return null;
	}
	this.isAscendantSort = function () {
		if (this.request.order.length > 0 )
			return this.request.order[0].direction =="ASC";
		return null;
	}
	
	// -----------------------------------------------------------------------------------------
	//  										Form
	// -----------------------------------------------------------------------------------------
	this.form = {'requestname':''};
	
	this.setForm = function ( ) {
		var sourceSelected=null;
		for (var i in this.saveinfo.listrequests)
		{
			var sourceI = this.saveinfo.listrequests[ i ];
			if (sourceI.name === this.form.requestname)
			{
				this.form.description = sourceI.description;
				sourceSelected = sourceI;
			}
		}
		this.loadRequest( sourceSelected );
	}
	
	
	this.isFormFieldVisible = function( fieldParameter ) {
		if (this.request.selection.type==='SQL')
			return true;
		if (fieldParameter.visible)
			return true;
		return false;
	}
	
	
	
	// -----------------------------------------------------------------------------------------
	//  										EditRecord
	// -----------------------------------------------------------------------------------------
	this.edit = { 'allowAddRecord' : false, 'listrecords': [], 'sourcename':'', 'listevents':''}
	
	
		
		
	var modalInstance=null;
	this.openModalRecord= function()
	{
		console.log("openDialog 2");
		$('#myModal').modal('show'); 
		
	}
	this.closeModalRecord = function()
	{
		$('#myModal').modal('hide'); 
		
	}

	/**
	 * calculate the list of field to add / edit a record
	 */
	this.getListFields = function( recordData, headers )
	{
		var listFields=[];
		for (var i in headers)
		{
			var oneHeader = headers[ i ]
			console.log('edit record header='+angular.toJson( oneHeader ));
			
			if (oneHeader.type== '_EDITRECORD')
				continue;
			
			var record = {'name': oneHeader.columnid, 'title': oneHeader.title,'type': oneHeader.type, 'value': '' };
			record.show=true;
			if (oneHeader.columnid == 'PersistenceId')
				record.show=false;
			record.value = recordData[ oneHeader.columnid ];
			
			listFields.push( record );
		
		}
		return listFields;
		
	}
	this.editRecord = function( recordData, headers, sourcename)
	{
		console.log('edit record '+sourcename);
		
		// prepare the data to display in the modal
		this.edit.listrecords=[];
		this.edit.sourcename 	= sourcename;
		this.edit.listrecords 	= this.getListFields( recordData, headers );
		this.edit.operation 	= 'UPDATE';

		this.openModalRecord();
		
	}
	this.addRecord = function( headers, sourcename)
	{
		// prepare the data to display in the modal
		this.edit.listrecords=[];
		this.edit.sourcename 	= sourcename;
		this.edit.listrecords 	= this.getListFields( {}, headers );
		this.edit.operation 	= 'INSERT';

		this.openModalRecord();
		
	}
	this.updateRecord = function( )
	{
		// call server tp update the record
		alert('Confirm the modification ');
		// this.edit.sourcename="com.airtahitinui.bpm.TNWaiverCode";
		var param={ 'request': { 'sourcename' : this.edit.sourcename },
				'listrecords': this.edit.listrecords};
		
		this.edit.listevents ='';
		
		// console.log("Save=");

		this.bigPost( param, "updaterecordbdm");

	}
	this.allowAddRecord = function()
	{
		return this.edit.allowAddRecord;
	}
	// -----------------------------------------------------------------------------------------
	//  										Execute Configuration Request
	// -----------------------------------------------------------------------------------------
	this.report ={'filterrecord':{}, 'nbrecords':0};
	this.runRequestConfiguration = function( output ) {
		var self = this;
		self.inprogress=true;

		// allow the addRecord if the type is EDITRECORD
		if (this.request.result.typeresult == 'EDITRECORD')
			this.edit.allowAddRecord=true;
		else
			this.edit.allowAddRecord=false;
		
		self.report.listevents ='';

		if (this.display.hidefilteratexecution)
			this.display.showfilter=false;
		var param={ 'request': this.getRequest(), 'output': output };
		console.log("RunRequest= startindex"+param.startindex+"] maxResult="+param.maxresults);
		
		 this.bigPost( param, "executeRequest");
	}


	// -----------------------------------------------------------------------------------------
	//  										bigPost mechanism
	// -----------------------------------------------------------------------------------------
	
	this.bigPost = function ( param, action)
	{
		this.inprogress						= true;
		// console.log("paramBigPost="+angular.toJson( param, false));
		var json= angular.toJson( param, false);
		
		console.log("~~~~~~~~~~~~~~ BigPost Send :"+json);

		
		// the array maybe very big, so let's create a list of http call
		this.listUrlCall=[];
		//this.listUrlCall.push( "action=collect_reset");
		this.listeventsexecution="";
		var self=this;
		self.lastaction=action;
		
		// split the string by packet of 5000 
		while (json.length>0)
		{
			var jsonFirst = encodeURIComponent( json.substring(0,2000));
			
			var url="action=";
			if (this.listUrlCall.length==0)
				url += "collect_reset"
			else
				url +="collect_add";
			url +="&paramjson="+jsonFirst;
			json =json.substring(2000);
			if (json.length==0)
				url +="&finalaction="+action;	
			
			this.listUrlCall.push( url );

		}
		// self.listUrlCall.push( "action="+action);
		
		self.listUrlIndex=0;
		self.executeListUrl( self ) // , self.listUrlCall, self.listUrlIndex );
	}
	
	
	this.executeListUrl = function( self ) // , listUrlCall, listUrlIndex )
	{
		// console.log(" Call "+self.listUrlIndex+" : "+self.listUrlCall[ self.listUrlIndex ]);
		self.listUrlPercent= Math.round( (100 *  self.listUrlIndex) / self.listUrlCall.length);
		var d = new Date();
		
		$http.get( '?page=custompage_moonrover&t='+d.getTime()+'&'+self.listUrlCall[ self.listUrlIndex ] )
			.success( function ( jsonResult ) {
				// console.log("Correct, for ["+self.listUrlCall[ self.listUrlIndex ] +"] jsonResult=["+angular.toJson(jsonResult)+"]");
				// angular.toJson(jsonResult));
				self.listUrlIndex = self.listUrlIndex+1;
				if (self.listUrlIndex  < self.listUrlCall.length )
					self.executeListUrl( self ) // , self.listUrlCall,
												// self.listUrlIndex);
				else
				{
					// console.log("Finish", angular.toJson(jsonResult));
					
					self.inprogress						= false;
					
					if (self.lastaction === 'executeRequest')
					{
						console.log("GetExecuteRequest answer-begin");
						self.report.listevents = jsonResult.listevents;
						self.report.listdata = jsonResult.listdata;
						self.report.listheader = jsonResult.listheader;
						self.report.listfooterdata = jsonResult.listfooterdata;
						self.report.nbrecords =  jsonResult.nbrecords;
						self.report.startindex =  jsonResult.startindex;
						self.report.maxresults =  jsonResult.maxresults;
						self.display.totallines= jsonResult.nbrecords;
						
						console.log("GetExecuteRequest answer-end");
						self.report.filterrecord={};
					}
					if (self.lastaction === 'saveRequest')
					{
						self.saveinfo.listrequests = jsonResult.listrequests;
						self.saveinfo.listevents = jsonResult.listevents;
					}
					if (self.lastaction === 'updaterecordbdm')
					{
						
						self.edit.listevents = jsonResult.listevents;
						console.log("updaterecordbdm-listevent="+jsonResult.listevents);
					}
					console.log("noonrover.loadSources : receive list");
				}
			})
			.error( function ( result ) {
					self.inprogress						= false;
					console.log("url In Error", angular.toJson(jsonResult));
					
					}
				);
	
	}
	
	// -----------------------------------------------------------------------------------------
	//  										Result fonction
	// -----------------------------------------------------------------------------------------
	this.getResultHeader = function() {
		return this.report.listheader;
		
	}
	this.getResultData = function() {
		return this.report.listdata;
	}
	this.getResultFooterData = function() {
		return this.report.listfooterdata;
	}
	
	
	this.getValue = function(record, header)
	{
		return record[ header.columnid ];
	}
	
	this.getLocalExcel = function () 
	{  
		//Start*To Export SearchTable data in excel  
	// create XLS template with your field.  
		var mystyle = {         
				headers:true,        
				columns: [ ]
			// { columnid: 'name', title: 'Name'},
			         
		};  
		var resultHeaderData=this.getResultHeader();
		for (var i in resultHeaderData)
		{
			console.log( "alasql:data="+angular.toJson(resultHeaderData[ i ]));
			
			var record ={'columnid': resultHeaderData[ i ].columnid, 'title': resultHeaderData[ i ].title};
			mystyle.columns.push( record );
		}
        //get current system date.         
        var date = new Date();  
        $scope.CurrentDateTime = $filter('date')(new Date().getTime(), 'MM/dd/yyyy HH:mm:ss');          
		var trackingJson = this.getResultData();
		// console.log( "alasql:header="+angular.toJson(mystyle));
		// console.log( "alasql:data="+angular.toJson(trackingJson));
		
        //Create XLS format using alasql.js file.  
        alasql('SELECT * INTO XLS("Data_' + $scope.CurrentDateTime + '.xls",?) FROM ?', [mystyle, trackingJson]);  
    };
	// -----------------------------------------------------------------------------------------
	//  										Pagination
	// -----------------------------------------------------------------------------------------
	this.display = {
			pagination : [ 
				 { "name": "100", 	   "value":100},
				 { "name": "500", 	   "value":500},
				 { "name": "1000", 	   "value":1000}
				],
			numberperpage:null,
			pagenumber:1,
			totallines:0,
			
			maxCsvValue:10000,
			
			showfilter: true,
			showadvance:true,
			hidefilteratexecution:true,
			showformsection:true,
			isadmin : false
	};
	this.display.numberperpage = this.display.pagination[ 1 ];
	
	this.getStepPagination = function() {
		return this.display.pagination;
	}

	// -----------------------------------------------------------------------------------------
	//  										tool
	// -----------------------------------------------------------------------------------------

	this.getListEvents = function ( listevents ) {
		console.log("getListEvents="+listevents);
		if (listevents == 'undefined')
			return "";
		return $sce.trustAsHtml(  listevents );
	}
	
	this.getButtonClass= function( isEnable )
	{
		if (isEnable)
			return "btn btn-success btn-xs"
		else
			return "btn btn-primary btn-xs"
	}
	// -----------------------------------------------------------------------------------------
	//  										Init
	// -----------------------------------------------------------------------------------------
	this.init = function()
	{
		var self=this;
		
		self.inprogress=true;
		//alert("source= "+ angular.toJson(self.simulation.listsources, true))
		//self.request.listsources = self.simulation.listsources; 
		console.log("noonrover.init");
		var param={};
		var json= encodeURIComponent( angular.toJson( param, true));
		var d = new Date();
		
		var url='?page=custompage_moonrover&action=init&paramjson='+json+'&t='+d.getTime();;
		$http.get( url )
				.success( function ( jsonResult ) {
							self.inprogress						= false;
							self.definition.listevents = jsonResult.listevents;
							self.definition.listsources = jsonResult.listsources;
							self.definition.listselections=[];

							self.saveinfo.listrequests = jsonResult.listrequests;
							self.display.isadmin = jsonResult.isadmin;
							self.display.showformsection = ! self.display.isadmin;
							
							self.isinitialised = true;
							
							console.log("noonrover.init : receive list");
							}
				)
				.error( function ( result ) {
							self.inprogress						= false;
							
							}

				);
	}
	
	this.init();

});

})();