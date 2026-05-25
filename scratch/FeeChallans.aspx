

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <!-- Showing Alert Message On Page load For feed Back-->

    <script type="text/javascript">
        function alertUser(msg) {
            alert(msg);
        }
    </script>

    <meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1" />
    <title>Student Console</title>
    <!--                       CSS                       -->
    <!-- Reset Stylesheet -->
    <link rel="stylesheet" href='/BOT_Theme/resources/css/reset.css'
        type="text/css" media="screen" />
    <!-- Main Stylesheet -->
    <link rel="stylesheet" href='/BOT_Theme/resources/css/style.css'
        type="text/css" media="screen" />
    <!--[if lte IE 7]>
			<link rel="stylesheet" href="BOT_Theme/resources/css/style-ie.css" type="text/css" media="screen" />
		<![endif]-->
    <!-- Invalid Stylesheet. This makes stuff look pretty. Remove it if you want the CSS completely valid -->
    <link rel="stylesheet" href='/BOT_Theme/resources/css/invalid.css'
        type="text/css" media="screen" />
    <!-- Colour Schemes
	  
		Default colour scheme is green. Uncomment prefered stylesheet to use it.-->
    <link rel="stylesheet" href='/BOT_Theme/resources/css/blue.css'
        type="text/css" media="screen" />
    <link rel="stylesheet" href='/BOT_Theme/resources/css/default.css'
        type="text/css" media="screen" />
    <link rel="stylesheet" href='/BOT_Theme/resources/css/debug.css'
        type="text/css" media="screen" />
    <link rel="stylesheet" href='/BOT_Theme/resources/css/spread.css'
        type="text/css" media="screen" />
    <!--<link rel="stylesheet" href="~/resources/css/red.css" type="text/css" media="screen" />  
	 
		-->
    <!-- Internet Explorer Fixes Stylesheet -->
    <!--[if lte IE 7]>
			<link rel="stylesheet" href="~/BOT_Theme/resources/css/spread.css" type="text/css" media="screen" />
		<![endif]-->
    <!--                       Javascripts                       -->
    <!-- jQuery -->

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/prototype.js"></script>

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/window.js"></script>

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/window_ext.js"></script>

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/effects.js"></script>

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/debug.js"></script>

    <script type="text/javascript" src="~/BOT_Theme/resources/scripts/extended_debug.js"></script>

     <script type="text/javascript" src='/_js/ECharts.js'></script>

    <script type="text/javascript" src='/BOT_Theme/resources/scripts/jquery-1.3.2.min.js'></script>

    <!-- jQuery Configuration -->

    <script type="text/javascript" src='/BOT_Theme/resources/scripts/simpla.jquery.configuration.js'></script>

    <!-- Facebox jQuery Plugin -->

    <script type="text/javascript" src='/BOT_Theme/resources/scripts/facebox.js'></script>

    <!-- jQuery WYSIWYG Plugin -->

    <script type="text/javascript" src='/BOT_Theme/resources/scripts/jquery.wysiwyg.js'></script>

    
    <!-- Internet Explorer .png-fix -->
    <!--[if IE 6]>
			<script type="text/javascript" src="~/resources/scripts/DD_belatedPNG_0.0.7a.js"></script>
			<script type="text/javascript">
				DD_belatedPNG.fix('.png_bg, img, li');
			</script>
		<![endif]-->

    <script type="text/javascript" language="javascript">
        function ShowResultCard() {
            var win = new Window({ className: "spread", title: "Ruby on Rails", top: 70, left: 100, width: 300, height: 200, url: "http://www.rubyonrails.org/", showEffectOptions: { duration: 1.5 } }); win.show();
        }

    </script>

    <script type="text/javascript" language="javascript">
        function CloseNotification() {
            var main = document.getElementById("ctl00_main"); main.style.opacity = 1;
        } </script>

    <style type="text/css">
        #progressBackgroundFilter {
            position: fixed;
            top: 0px;
            bottom: 0px;
            left: 0px;
            right: 0px;
            overflow: hidden;
            padding: 0;
            margin: 0;
            background-color: #000;
            filter: alpha(opacity=50);
            opacity: 0.5;
            z-index: 1000;
            height: 100%;
        }

        #processMessage {
            position: fixed;
            top: 200px;
            left: 43%;
            padding: 10px;
            width: 14%;
            z-index: 1001;
            background-color: #fff;
            text-align: center;
        }

        .ddlClass {
            font-size: 18px;
            height: 30px;
            line-height: 30px;
            width: 100px; /*
scrollbar-face-color: #FFCCCC;
scrollbar-highlight-color: #4B4B4B;
scrollbar-3dlight-color: #646464;
scrollbar-darkshadow-color: #969696;
scrollbar-shadow-color: #C0C0C0;
scrollbar-arrow-color: #090925;
scrollbar-track-color: #051011;*/
        }

        .pgr {
            background: #424242 url(grd_pgr.png) repeat-x top;
        }

            .pgr table {
                margin: 5px 0;
            }

            .pgr td {
                border-width: 0;
                padding: 0 6px;
                border-left: solid 1px #666;
                font-weight: bold;
                color: #fff;
                line-height: 12px;
            }

            .pgr a {
                color: #666;
                text-decoration: none;
            }

                .pgr a:hover {
                    color: #000;
                    text-decoration: none;
                }
        /*by adil Start*/ .Initial {
            display: block;
            padding: 2px 10px 4px 12px;
            float: left;
            background: url("../images/InitialImage.png") no-repeat right top;
            color: Black;
            font-weight: bold;
        }

            .Initial:hover {
                color: White;
                background: url("../images/SelectedButton.png") no-repeat right top;
            }

        .Clicked {
            float: left;
            display: block;
            background: url("../images/SelectedButton.png") no-repeat right top;
            padding: 2px 10px 4px 12px;
            color: Black;
            font-weight: bold;
            color: White;
        }
        /*by adil End*/
    </style>
</head>
<body>
    <form method="post" action="./FeeChallans.aspx" id="MasterForm">
<div class="aspNetHidden">
<input type="hidden" name="__EVENTTARGET" id="__EVENTTARGET" value="" />
<input type="hidden" name="__EVENTARGUMENT" id="__EVENTARGUMENT" value="" />
<input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="WJIy/NkJkn3UvO5z88iV7WcV48zxf6Yll6B/BCuTH9Hl54V/uIhwxFloNWyVHCK3pwMP94gZ9n15O6SegZYJgltmhoDVT4Io51JWIv2c5Av4Tt23JcX+DhH8UE/jty/q3V4I7KMsuJs/OV/LSLxy2BPDWu3Fv3v+H5amCZACFuw9HkGL0J8fTlyxb0uhRN59indXpk2oj8i+WTUma/1M86Kje9ZtqbBdq3Fwpy8ZwzsbtpNbui8PX4EcPcgDjJRrqJjFPBYjzfi+SvVbiDZRgAgt3zvh1dFo/5xtogWKt/pp1w/J0DH43lhBfOfsPi6iBOwYHnd4r0Z9JsHVuOQMU0yovGVzS36DlOzeDKVOkWds2zVnYKVwOD4Vu0CWWm/LDGDA4k7WodXPUhwEj4gyhhvspeTMlSMG+StWSFk0iazF4pEl5vQ3LG7XRzyEJWa8/mXbS7Hw1dLCotav7klZlfgyeaH8Y4k8r4pNoAXeqmsUgghA7fp+u5kkwkyB7NuZjVRu3OQD0aDhqsGuA+bSQDFIW1wPl4JmJlqqtgMmjVkXnCztsZElexVaKlN3XSAfIeUxbi+bMSZgjL6G3qfCLKnfVC4BuM/wOU1jWxx1UAPgmlTpI3KphLZX8k5MznnGHd/4pm0/sjmlb6OPHW1s/lQrGrMY5Z9bgwapOj5B3dfuGzrrMW1YtDP75PURQM6F1ywrSac9E8pJkf1FzOyBToTM2WFh3MgZ/qVZnkCxxCNj4W/mLrco3fLDh3Sdqw0Jz62mjTpecCQjgCsrYWFeGqI4LerF49qeZE/QaALl74LEeQEDncNsGeyyFUAxq3RzXk9gYe2Il8/Hd2KUXCEHIVndZbGrL5puueXFT/mx6T5KrMTO+BiohODAzcJPdmqvVmwhdm1KcrGB2bSPD0v8xm68UcGbQIRoeoY2AVGFJw9zlzjFIYhoSU2wTh8eJu0XcA8NCewb+3jbRMazWiTCDFMIHELdN45KNpTNh70CgN6SSx65fPS2Oy/fmyURTrxgrXrVFeO/+brTEbqb4JUfKe2bbsC6Jc89016BV44Ls2Oc34ojGahRJEuAtKWG0lXUGmSKY7kX2uADtiRGtPIU+dSTN68nUOb9qxE12UlFDrkxU2dvwnVjxWvSGZLJ5pwZmCNMDVCCepBpdcwtWSJg1K1r/evNj6ur5YcXmiX9O+kQx3rJx9FxmVyj7hqYGGO5MlhF3Hslp2frCb3hqOx3x0DLqXAAqYNFDtBGZ4aWhgq/Oog96U+xixqi2u8zKRSqua5Zp7Zu+RuLrOi9HXiQ5g==" />
</div>

<script type="text/javascript">
//<![CDATA[
var theForm = document.forms['MasterForm'];
if (!theForm) {
    theForm = document.MasterForm;
}
function __doPostBack(eventTarget, eventArgument) {
    if (!theForm.onsubmit || (theForm.onsubmit() != false)) {
        theForm.__EVENTTARGET.value = eventTarget;
        theForm.__EVENTARGUMENT.value = eventArgument;
        theForm.submit();
    }
}
//]]>
</script>


<script src="/WebResource.axd?d=55IxIhH_rySkSOMLM6i--J-kjHNki9zh1TtBbDW9fjvdExDsPOO5mn7q_jjmZ9n6aYRsMC7aRoT6zRta1LhVmpGmoW0OvW6j5owdYHdNPlg1&amp;t=638901721900000000" type="text/javascript"></script>


<script src="/ScriptResource.axd?d=zYrh4YumfVVAwpblcsqtauzX8sUqhDmJnK2kV2_T3gtzVvDZcQmtNijLf_JJyoMcptqjs_XhQ66GzAcw5H80X6DeGFstk5Twz8V6xsaTJ-u0511VDgZ-n98Rq2m-DweHLqJTHauWV0f7yJQOuW6bDUZuE7ya66DbYyZwGPhVKVg1&amp;t=5c0e0825" type="text/javascript"></script>
<script src="/ScriptResource.axd?d=LxgwgNG_Gd4Sfgshg6mL2CP6IJKABnl_AZavJ4QtVpCl2TdPM3FPWPe4qIk6HtzatDXPDh0P7Ekj8jQp0ljnpNHqjqwvSnY1w3y0oiH5hNc0J8FzLyl4Mx7qZGI2QXcpVtHTZtVcanH-3OdZZtoj3i1fputdoQ-jaIyljx8uE5LNlF68NPQfvYx1-R001aQG0&amp;t=5c0e0825" type="text/javascript"></script>
<div class="aspNetHidden">

	<input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="6CC25204" />
	<input type="hidden" name="__VIEWSTATEENCRYPTED" id="__VIEWSTATEENCRYPTED" value="" />
	<input type="hidden" name="__PREVIOUSPAGE" id="__PREVIOUSPAGE" value="Fd4SM678oKDTFUNpLe1G1fQXPegaf9CmLKacnz5PfkdZw6vrbWqfpETe0r0HkDILv9auGkOPbBi-aTBMyvlZnjNRe8jUuPe058tG4XruGco1" />
	<input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="B73sDB5lmQ/wVKXd4bFDZQDJvGUPWA6V0i9SaAdacCUucqdh8a/CnfeRe4lTcPC0NFIZMZWFybpdvWIv4IV1WnDBkHP+/jbxb95FbwWGPknrQmT7Ayf7RH4Iq2i+JpCAnxEnKO83UYyQPD6JGsj+sv+xWZEttJE7hdwlfFrQ3ZL3khGv6gR8a2PGsccrBCOwHfxWj4SnQmpx7sBIW8S4mt+XcUMoC7PZ2RHGKCAgMEB4OxXjFeCTsi2PlCriSj5F3O1CITmwy6G/9ql9Y1/ILuXD8kAeivAGQnRpQ38M/+mQzXqhfUlJMGH2J9AQFc2+W01cg+ZUOeXaX8Z49Z67yXgyxEJ1P7r/OCXGW7raHr3J9vXw0CbjK4Ggl5Cz+4wwVo5qPYcSIrwDwkBMRun24TuzTSl4i+4A5N3hVVMRMOFrbTlSsxDblQ3O06B+xaTXbtCfu3v0vYvAzmX0/hV6RYYlgGBwRqisL7/F3FZW0OhdWa0hUbUomwk5gN8acNfD8p0nj14Mek2BDLdS5LAuLVo0OYcHWOVG5K3dlSC/1f1lYAypW0TRf92mtgwuGgDoikh3cc0mzk/wUeqPuavq7jOLwFrN4FoLElQSVwAUNSsRXMYU1VZ4+q2RuaNwQ/AelvoypjkQgs3rAb5g6FbGHF4dc5S1+aa1UZxmOHG4yGA=" />
</div>
        
        
        
        
        
        <div id="main">
            <script type="text/javascript">
//<![CDATA[
Sys.WebForms.PageRequestManager._initialize('ctl00$Manager', 'MasterForm', [], [], [], 90, 'ctl00');
//]]>
</script>

            
            <div id="body-wrapper">
                <!-- Wrapper for the radial gradient background -->
                <div id="sidebar">
                    <div id="sidebar-wrapper">
                        <!-- Sidebar with logo and menu -->
                        <h1 id="sidebar-title">
                            <a href="#">Student COnsole</a></h1>
                        <!-- Logo (221px wide) -->
                        <center>
                        <a href="#">
                            <img id="logo" src="resources/images/CIITLogo_Plain.png" alt="COMSATS" />
                            </a>
                    </center>
                        <!-- Sidebar Profile links -->
                        <div id="profile-links" style="text-align: center;">
                            <span id="Label1" style="font-size:10pt;">Welcome,</span>
                            <a href="#" title="Edit your profile">
                                <span id="lbl_StudentName" style="font-size:10pt;">Danial Ahmed</span></a><br />
                            <br />
                            <a id="lnk_Notifications" title="Notifications" href="javascript:__doPostBack(&#39;ctl00$lnk_Notifications&#39;,&#39;&#39;)" style="font-size: 10pt">Notifications</a>
                            |
                        <a id="lnk_Signout" href="javascript:__doPostBack(&#39;ctl00$lnk_Signout&#39;,&#39;&#39;)" style="font-size: 10pt">Sign 
                        Out</a>
                            <br />
                            
                            <a href="StudentConsoleManual.zip" style="font-size: 9pt; color: Yellow">User Manual</a>
                            |
                            |
                            |
                        </div>
                        <ul id="main-nav">
                            <!-- Accordion Menu -->
                            <li>
                                <a id="lnk_Dashboard" class="nav-top-item no-submenu" href="javascript:__doPostBack(&#39;ctl00$lnk_Dashboard&#39;,&#39;&#39;)">Dashboard</a>
                            </li>
                            <li id="lst_MSteam"><a id="lnkMSteamMain" class="nav-top-item">
                                <!-- Add the class "current" to current menu item -->
                                Microsoft Office 365 / Teams </a>
                                <ul>
                                    <li>
                                        <a id="lnkMSTeam" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkMSTeam&quot;, &quot;&quot;, false, &quot;&quot;, &quot;StudentsDetails/StudentNotices.aspx&quot;, false, true))"> Account Details</a>
                                    </li>
                                </ul>
                            </li>

                             <li id="lstSurveyFeedback"><a href="#" id="lnkSurveyFeedback" class="nav-top-item" style="display:none;">Survey Feedback</a>
                                <ul>                              

                                    <li>
                                        <a id="LinkSurveyFeedback" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$LinkSurveyFeedback&quot;, &quot;&quot;, false, &quot;&quot;, &quot;SurveyFeedback/FeedbackAnswersByStudent.aspx&quot;, false, true))" style="display:none;">Start Survey Feedback</a>
                                    </li>
                                </ul>
                            </li>
                            


                            

                            <li id="lstSynopsis" style="display:none">
                                
                                <a href="#" id="A1" class="nav-top-item">Synopsis Submission</a>
                                <ul>
                                    <li>
                                         <a href="StudentsDetails/EFSRedirect.aspx" id="lnkSynopsis" target="_blank" rel="noopener noreferrer">Synopsis Submission </a>
                                    </li>
                                   
                                </ul>
                            </li>


                            <li id="lstRegistration">
                                
                                
                                <ul>
                                    <li>
                                        
                                    </li>

                                    <li>
                                        
                                    </li>

                                    <li>
                                        </li>
                                </ul>
                            </li>
                            
                            <li id="lstCourses"><a id="lnk_Courses" class="nav-top-item">
                                <!-- Add the class "current" to current menu item -->
                                Courses </a>
                                <ul>
                                    <li>
                                        <a id="lnk_Summary" href="javascript:__doPostBack(&#39;ctl00$lnk_Summary&#39;,&#39;&#39;)">Summary</a></li>
                                    <li>
                                        <a id="lnk_ClassProceedings" href="javascript:__doPostBack(&#39;ctl00$lnk_ClassProceedings&#39;,&#39;&#39;)">Class Proceedings</a></li>
                                    <li>
                                        <a id="lnk_QAMarks" href="javascript:__doPostBack(&#39;ctl00$lnk_QAMarks&#39;,&#39;&#39;)">Q.A/Sess/Final 
                                    Marks</a></li>
                                    <!-- Add class "current" to sub menu items also -->
                                </ul>
                            </li>
                            <li id="lstCoursePortal"><a id="lnk_CoursePortal" class="nav-top-item">Course Portal </a>
                                <ul>
                                    <li><a href="CTS/CTSdashboard.aspx" id="lnkOnlineTest">MCQ Test</a></li>
                                    <li><a href="CoursePortal.aspx?isTest=1" id="lnkSubjectiveTests">Subjective Test</a></li>
                                    <li><a href="CoursePortalContentsSummary.aspx" id="lnkCourseContents">Course Contents</a></li>
                                    <li><a href="CoursePortal.aspx" id="lnkPortalSummary">Assignments Summary</a></li>
                                    <li><a href="CoursePortalPendingAssignments.aspx" id="lnkPendingAssignments">Pending Assignments</a></li>
                                </ul>
                            </li>

                             <li><a href="#" id="lnkEntryCoupon" class="nav-top-item">Exam EntryCoupon</a>
                                <ul>
                                    <li><a href="EntryCouponSelect.aspx" id="lnkEntryCouponQR">Exam EntryCoupon Print
                                    </a></li>
                                </ul>
                            </li>

                            <li><a href="#" id="lnkStudentApp" class="nav-top-item">Student Mobile App Activation</a>
                                <ul>
                                    <li><a href="GenerateAppPassword.aspx" id="lnkStudentAppActivation">Student App Activation</a></li>
                                </ul>
                            </li>

                           
                            <li><a href="#" id="lnkPositionHolders" class="nav-top-item" style="display:none;">Position
                            Holders Registration</a>
                                <ul>
                                    <li><a href="PositionHoldersRegistration.aspx" id="lnkPositionHoldersReg" style="display:none;">Position Holders Registration</a></li>
                                </ul>
                            </li>
                            <li><a href="#" id="lnkFunkadaRegistration" class="nav-top-item" style="display:none;">Funkada
                            Registration</a>
                                <ul>
                                    <li><a href="FunkadaETicking.aspx" id="lnkFunkadaRegister" style="display:none;">Funkada Registration</a></li>
                                </ul>
                            </li>
                            <li><a href="#" id="lnkBoarding" class="nav-top-item">Boarding</a>
                                <ul>
                                    <li><a href="RequestBoarding.aspx" id="lnkRequestBoarding">Request Boarding</a></li>
                                </ul>
                            </li>
                            <li><a href="#" id="lnk_Sibling" class="nav-top-item">Sibling Info</a>
                                <ul>
                                    <li>
                                        <a id="lnk_SiblingInfo" href="javascript:__doPostBack(&#39;ctl00$lnk_SiblingInfo&#39;,&#39;&#39;)">Add Sibling Info</a>
                                    </li>
                                    
                                </ul>
                            </li>
                            <li><a href="#" id="lnk_Result" class="nav-top-item">Result</a>
                                <ul>
                                    <li>
                                        <a id="lnk_ResultCard" href="javascript:__doPostBack(&#39;ctl00$lnk_ResultCard&#39;,&#39;&#39;)">Result 
                                    Card</a>
                                    </li>
                                </ul>
                            </li>
                            <li><a href="#" id="lnkOBE" class="nav-top-item">OBE</a>
                                <ul>
                                    <li><a href="PLOSummary.aspx" id="lnkPLOSummary">Program PLO Details</a></li>
                                    <li><a href="CurrentSemesterCLO.aspx" id="lnkCurrentCLO">Current Semester
                                    CLO</a></li>
                                    <li><a href="semesterwiseCLO.aspx" id="lnkSemesterwiseCLO">Semester wise
                                    CLO </a></li>
                                    <li><a href="PLOAchievement.aspx" id="lnkOverAllPLO">PLO Achievement</a></li>
                                </ul>
                            </li>
                            <li id="lstProject"><a href="#" id="lnkProjectMain" class="nav-top-item" style="display:none">Projects </a>
                                <ul>
                                    <li><a href="FinalProject/ApprovedProjects.aspx" id="lnkApprovedProjects">Approved Projects</a></li>
                                    <li><a href="FinalProject/DocumentTemplates.aspx" id="lnkTemplates">Document Templates</a></li>
                                    <li><a href="FinalProject/ProjectComments.aspx" id="lnkProjectTasks" style="display:none">Project Tasks</a></li>
                                </ul>
                            </li>
                            <li id="lstTimetable"><a href="#" id="lnkTimeTableMain" class="nav-top-item">Time Table </a>
                                <ul>
                                    <li><a href="TimeTable.aspx" id="lnkTimeTable">TimeTable</a></li>
                                </ul>
                            </li>
                            <li visible="false"><a href="#" id="lnk_FeeChallan" class="current nav-top-item">Fee </a>
                                <ul>
                                    <li><a href="FeeChallans.aspx" id="lnkFee" class="current">Challan</a></li>
                                    <li><a href="FeeHistorySFMS.aspx" id="lnkFeeHistory">History</a></li>
                                    <li></li>
                                    <li></li>
                                </ul>
                            </li>
                            
                            <li><a id="lnkLibraryMain" class="nav-top-item">Library</a>
                                <ul>
                                    <li><a href="BooksReserved.aspx" id="lnkReserveBook">Book Reservation</a></li>
                                    <li><a href="BookBorrowHistory.aspx" id="lnkBookBorrowHistory">Book Borrow
                                    History</a></li>
                                </ul>
                            </li>
                            <li style="display: none"><a id="lnkGRASSPMain" class="nav-top-item">GRASSP</a>
                                <ul>
                                    <li><a href="GraSPP/GraSPPStudentInfo.aspx" id="stuinfo">Student Info</a></li>
                                    <li><a href="GraSPP/GraSPPBasicInfoBASRTests.aspx" id="stuinfo1">BasicInfo
                                    </a></li>
                                    <li><a href="GraSPP/ComposeCommittee.aspx" id="A2">ComposeCommittee</a></li>
                                    <li><a href="GraSPP/GraSPP_Print.aspx" id="A3">GraSPP_Print </a></li>
                                    <li><a href="GraSPP/GraSPPStudentInfo.aspx" id="A4">Student Info</a></li>
                                    <li><a href="GraSPP/GraSPPBasicInfoBASRTests.aspx" id="A5">BasicInfo
                                    </a></li>
                                    <li><a href="GraSPP/GraSPPCompletion.aspx" id="A6">GraSPPCompletion</a></li>
                                    <li><a href="GraSPP/GraSPPComprehensieExam.aspx" id="A7">GraSPPComprehensieExam
                                    </a></li>
                                    <li><a href="GraSPP/GraSPPRemarks.aspx" id="A8">GraSPPRemarks</a></li>
                                    <li><a href="GraSPP/GraSPPResearchActivityProblemRelevantInfo.aspx" id="A9">ResearchActivityProblem </a></li>
                                    <li><a href="GraSPP/GraSPPShowResultCard.aspx" id="A10">GraSPPShowResultCard</a></li>
                                    <li><a href="GraSPP/GraSPPSubmitForm.aspx" id="A11">GraSPPSubmitForm
                                    </a></li>
                                    <li><a href="GraSPP/GraSPPSynopsisSeminars.aspx" id="A12">GraSPPSynopsisSeminars</a></li>
                                    <li><a href="GraSPP/GraSPPThesisMeeting.aspx" id="A13">GraSPPThesisMeeting
                                    </a></li>
                                </ul>
                            </li>
                            <li>
                                <ul>
                                    <li>
                                        <a id="lnk_FeedbackSummary" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnk_FeedbackSummary&quot;, &quot;&quot;, false, &quot;&quot;, &quot;StudentFeedBackSummary.aspx&quot;, false, true))">Summary</a></li>
                                </ul>
                            </li>
                            <li id="lstConvocation"><a href="#" id="lnkConvocationRegistration" class="nav-top-item">Convocation Registration </a>
                                <ul>
                                    <li>
                                        <a id="lnkRegister" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkRegister&quot;, &quot;&quot;, false, &quot;&quot;, &quot;ConvocationRegistration.aspx&quot;, false, true))">Register</a></li>
                                    <li>
                                        <a id="lnkConvPrint" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkConvPrint&quot;, &quot;&quot;, false, &quot;&quot;, &quot;ConvocationPrint.aspx&quot;, false, true))">Print Convocation Letter</a></li>
                                    <li>
                                        <a id="lnkConvPicture" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkConvPicture&quot;, &quot;&quot;, false, &quot;&quot;, &quot;ConvocationPicture.aspx&quot;, false, true))">Convocation Picture</a></li>
                                </ul>
                            </li>
                            <li id="lstScholarShip"><a href="#" id="lnkScholarships" class="nav-top-item">Scholarship</a>
                                <ul>
                                    <li>
                                        <a id="LinkRegform" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$LinkRegform&quot;, &quot;&quot;, false, &quot;&quot;, &quot;scholarship/ViewScholarshipFormLinks.aspx&quot;, false, true))" style="display:none;">Apply</a></li>
                                    <li>
                                        <a id="LinkScholarshipform" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$LinkScholarshipform&quot;, &quot;&quot;, false, &quot;&quot;, &quot;scholarship/ScholarshipFormLinksForView.aspx&quot;, false, true))" style="display:none;">View Form</a></li>
                                    <li>
                                        <a id="LinkViewScholarshipStatus" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$LinkViewScholarshipStatus&quot;, &quot;&quot;, false, &quot;&quot;, &quot;scholarship/ViewScholarshipStatuse.aspx&quot;, false, true))">View Scholarship Status</a></li>
										
										<li><a href="scholarship/Genral%20Scholarships%20Conditions%20April%202026.pdf" id="A14" target="blank">Genral Scholarships Conditions</a></li>
										
                                </ul>
                            </li>

                            <li id="lstClearanceMain"><a href="#" id="lnkClearanceMain" class="nav-top-item">University Clearance</a>
                                <ul>
                                    <li>
                                        <a id="lnkApplyForClearance" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkApplyForClearance&quot;, &quot;&quot;, false, &quot;&quot;, &quot;Clearance/AddStudentClearanceDetails.aspx&quot;, false, true))">Apply</a></li>
                                    <li>
                                        <a id="lnkClearanceStatus" href="javascript:WebForm_DoPostBackWithOptions(new WebForm_PostBackOptions(&quot;ctl00$lnkClearanceStatus&quot;, &quot;&quot;, false, &quot;&quot;, &quot;Clearance/ViewClearanceByUser.aspx&quot;, false, true))">View Status</a></li>
                                </ul>
                            </li>
                            <li id="lstGraduateProfile">
                                <a id="lnkGraduateProfile" class="nav-top-item no-submenu" href="javascript:__doPostBack(&#39;ctl00$lnkGraduateProfile&#39;,&#39;&#39;)">Graduate Progress Profile</a>
                            </li>
                            <li id="lstAPS"><a href="#" id="lnkapsmain" class="nav-top-item">Application Processing System</a>
                                <ul>
                                    <li>
                                        <a id="lnkapsprocess" href="javascript:__doPostBack(&#39;ctl00$lnkapsprocess&#39;,&#39;&#39;)">Apply</a>
                                    </li>
                                    <li>
                                        <a id="lnkapsstatus" href="javascript:__doPostBack(&#39;ctl00$lnkapsstatus&#39;,&#39;&#39;)">Application Status</a></li>
                                    <li>
                                        <a id="lnkcheckcoursestatus" href="javascript:__doPostBack(&#39;ctl00$lnkcheckcoursestatus&#39;,&#39;&#39;)">Course Application Status</a></li>
                                </ul>
                            </li>
                            <li><a href="#" id="lnk_Settings" class="nav-top-item">Settings</a>
                                <ul>
                                    <li>
                                        <a id="lnk_Profile" href="javascript:__doPostBack(&#39;ctl00$lnk_Profile&#39;,&#39;&#39;)">Profile</a>
                                    </li>
                                    <li>
                                        <a id="lnk_ChangePassword" href="javascript:__doPostBack(&#39;ctl00$lnk_ChangePassword&#39;,&#39;&#39;)">Change 
                                        Password</a></li>
                                    <li>
                                        <a id="lnk_LoginHistory" href="javascript:__doPostBack(&#39;ctl00$lnk_LoginHistory&#39;,&#39;&#39;)">Login 
                                        History</a></li>
										 
                                </ul>
                            </li>
                        </ul>
                        <!-- End #main-nav -->
                        <div id="messages" style="display: none">
                            <!-- Messages are shown when a link with these attributes are clicked: href="#messages" rel="modal"  -->
                            <h3>3 Messages</h3>
                            <p>
                                <strong>17th May 2009</strong> by Admin<br />
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus magna. Cras in
                            mi at felis aliquet congue. <small><a href="#" class="remove-link" title="Remove message">Remove</a></small>
                            </p>
                            <p>
                                <strong>2nd May 2009</strong> by Jane Doe<br />
                                Ut a est eget ligula molestie gravida. Curabitur massa. Donec eleifend, libero at
                            sagittis mollis, tellus est malesuada tellus, at luctus turpis elit sit amet quam.
                            Vivamus pretium ornare est. <small><a href="#" class="remove-link" title="Remove message">Remove</a></small>
                            </p>
                            <p>
                                <strong>25th April 2009</strong> by Admin<br />
                                Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus magna. Cras in
                            mi at felis aliquet congue. <small><a href="#" class="remove-link" title="Remove message">Remove</a></small>
                            </p>
                        </div>
                        <!-- End #messages -->
                    </div>
                </div>
                <!-- End #sidebar -->
                <div id="main-content">
                    <!-- Main Content Section with everything -->
                    <noscript>
                        <!-- Show a notification if the user has disabled javascript -->
                        <div class="notification error png_bg">
                            <div>
                                Javascript is disabled or is not supported by your browser. Please <a href="" title="Upgrade to a better browser">upgrade</a> your browser or <a href="" title="Enable Javascript in your browser">enable</a>
                                Javascript to navigate the interface properly.
                            </div>
                        </div>
                    </noscript>
                    <!-- Page Head -->
                    <div id="divStudentDetails">
                        <ul class="shortcut-buttons-set">
                            <li><a class="shortcut-button" href="#">
                                
                                
                                <img src="PictureHandler.ashx?reg_no=CIIT/SP25-BCS-136/ATD" id="stImg" alt="icon" style="width: 100px; height: 100px" />
                            </a></li>
                            <li style="width: 82%;">
                                <div class="studentdetails">
                                    <table cellpadding="0" cellspacing="0" class="Grid">
                                        <tr>
                                            <td style="width: 25%;" class="GridColumn">
                                                <b>Name :</b>
                                            </td>
                                            <td style="width: 30%;" class="GridColumn">
                                                <span id="lbl_Name">Danial Ahmed</span>
                                            </td>
                                            <td style="width: 20%;" class="GridColumn">
                                                <b>Roll No :</b>
                                            </td>
                                            <td style="width: 25%;">
                                                <span id="lbl_RollNo">CIIT/SP25-BCS-136/ATD</span>
                                            </td>
                                        </tr>
                                        
                                        <tr>
                                            <td class="GridColumn">
                                                <b>Father Name :</b>
                                            </td>
                                            <td class="GridColumn">&nbsp;<span id="lbl_FatherName">Sahibzada Muhammad Ishaq</span>
                                            </td>
                                            <td>
                                                <b>Registered Courses :</b>
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lbl_RegisteredCourses">5</span>
                                                
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="GridColumn">
                                                <b>Total Registered Courses :</b>
                                                <br />
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lbl_TotalRegisteredCourses">16</span>
                                                <span id="lbl_CurrentSelectedCourse"></span>
                                            </td>
                                            <td class="GridColumn">
                                                <b>Program :</b>
                                                <br />
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lbl_ProgramName">BCS</span>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td class="GridColumn">
                                                <b>Current Section :</b>
                                                <br />
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lbl_CurrentSection">C </span>
                                            </td>
                                            <td class="GridColumn">
                                                <b>Current Advisor :</b>
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lblStuAdv">NA</span>
                                            </td>

                                        </tr>
                                        <tr>
                                            <td class="GridColumn">
                                                <b>Date of Birth :</b>
                                            </td>
                                            <td class="GridColumn">
                                                
                                                <span id="lblDoB">Feb 12, 2006</span>
                                            </td>

                                            <td class="GridColumn">
                                                <b>CNIC :</b>
                                            </td>
                                            <td class="GridColumn">
                                                <span id="lblNID">16202-2007142-7</span>
                                            </td>
                                        </tr>
                                        <tr>
                                             <td class="GridColumn">
                                                <b>Thesis Title :</b>
                                            </td>
                                            <td class="GridColumn" colspan="3">
                                                
                                                <strong>
                                                <span id="lblThesisTitle">NA</span>
                                                </strong>
                                            </td>

                                        </tr>
                                        <tr>
                                            <td class="GridColumn" colspan="2">
                                                 <a id="HyperLink1" href="Clearance/MissingDocsAndDisciplinaryCase.aspx" target="_blank">Missing Documents / Disciplinary Case</a></td>

                                            <td class="GridColumn">
                                                &nbsp;</td>
                                            <td class="GridColumn">
                                                &nbsp;</td>
                                        </tr>
                                    </table>
                                </div>
                            </li>
                        </ul>
                    </div>
                    <div class="clear">
                    </div>
                    <div class="content-box" align="center">
                        <!-- Start Content Box -->
                        <div class="content-box-header">
                            
    <h3>
        Fee Challans</h3>

                            <div class="clear">
                            </div>
                        </div>
                        
    <br />
    <div id="DataContent_divmessage" class="notification information png_bg">
      <div style="display: inline-flex;
            align-items: center;
            font-size: 20px; /* Adjust size as needed */
            color: #007bff; /* Change color to your preference */
            text-decoration: none;
            font-weight: bold;">
            How to Pay 1Bill Invoice:  <a href="https://sis.cuiatd.edu.pk/1billhelp/"  target="_blank">
                
                Click Here
            </a>
        </div>
        </div>
    <br />
    <div>

</div>
    <br />

                        <!-- End .content-box-header -->
                        
                        <!-- End .content-box-content -->
                    </div>
                    <!-- End .content-box -->
                    <div class="clear">
                    </div>
                    <!-- Start Notifications -->
                    
                    
                    
                    
                    <!-- End Notifications -->
                    <div id="footer">
                        <small>Copyright © 2026 Software House, COMSATS University
 <span lang="en-us">
                            <span id="LblCampusName" style="font-size:Smaller;">Abbottabad</span> Campus</span>
                            |
                        
                            <a href="#">Top</a></small>
                    </div>
                    <!-- End #footer -->
                </div>
                <!-- End #main-content -->
            </div>
            
        </div>
    </form>
</body>
</html>
