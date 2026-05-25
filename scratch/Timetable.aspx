

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
    <form method="post" action="./Timetable.aspx" id="MasterForm">
<div class="aspNetHidden">
<input type="hidden" name="__EVENTTARGET" id="__EVENTTARGET" value="" />
<input type="hidden" name="__EVENTARGUMENT" id="__EVENTARGUMENT" value="" />
<input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="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" />
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

	<input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="6D10C4B0" />
	<input type="hidden" name="__PREVIOUSPAGE" id="__PREVIOUSPAGE" value="PtIsLSwdvSjEtzMDj53NanOcenrHRUDgsrWZvyXI8Zmy26pua7abIvsg3eYox9Zo1LIzceP7DWxFltSWymNR-iBHA2gNJaLLeOiScLiIwUg1" />
	<input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEdABt08SGvZgiRnu7pO+nceJ9Nt117de9GXZItS/j0v5c8FAMAAazT+/WMwZRQd71Lh46ugUTu1xeqYaRkm2Q5sR6iRhZ09OUrkfKhyha3KQpix3Ghmbcm8utIcFjcCf3SrTj2ZuwxwA2ZhIUoPU+DCjPxW3REJ/F07wGnNgodfQlQb5F5pKCRCrdGHd7EG/oEndV1I2CyfDO5H6x+WIM7Zpvom18ay5/kywJJUeFNU2t9hYpQ1iWH3xqwmUEGZ6zrDenDvQp+7X3YiUVD4dzSk8ziuwamUWN9+dlFPzVGUVPc+hi7DHwV9jSlTxJ8/Tw9xETLZSFlVVfiO83Ng6WPYkZnfdW7s7nH9JqmDbFa0I8jJKPehRK32mASjyTKlFs2n94xq4iLQrDpP2vxU2q2qBIwEf2XmFW/yVE/iymIm/jHUEUHgUG2my0PvfAJFMftpMsHqV8iLps8pOhxwkLRFBdxDjbVml4GmYejckKb4NA7YIWR5uZhEb9nzG6GnYt/3wgtc9ISIVGDqQFC+isLkdBX7PtoyH61LMi2U9XM42C0LJ/WDIR1xLO01LhuglnD539rxbcOb93EIT/3f+imui2+Z8vlOdGO5DJrFqCMONYZnQ==" />
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
                            <li id="lstTimetable"><a href="#" id="lnkTimeTableMain" class="current nav-top-item">Time Table </a>
                                <ul>
                                    <li><a href="TimeTable.aspx" id="lnkTimeTable" class="current">TimeTable</a></li>
                                </ul>
                            </li>
                            <li visible="false"><a href="#" id="lnk_FeeChallan" class="nav-top-item">Fee </a>
                                <ul>
                                    <li><a href="FeeChallans.aspx" id="lnkFee">Challan</a></li>
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
                            
<h3>Student Time Table</h3>

                            <div class="clear">
                            </div>
                        </div>
                        
<br />
 <div id="DataContent_dvMessage" class="notification information png_bg">
        <a href="#" class="close">
            <img src='/resources/images/icons/cross_grey_small.png' title="Close this notification"
                alt="close" onclick="CloseNotification();" /></a>
        <div id="DataContent_lblMessage">
        Please check your time table regularly. 
        </div>
    </div>
    <br />
  <div>
	<table class="table" cellspacing="0" rules="all" id="DataContent_gvTimeTable" style="border-color:#D8D8D8;border-width:1px;border-style:solid;font-family:Arial;font-size:X-Small;width:99%;border-collapse:collapse;table-layout: fixed;">
		<tr style="height:50px;">
			<th scope="col">DayTitle</th><th scope="col">10:00 to 10:30</th><th scope="col">10:30 to 11:00</th><th scope="col">11:00 to 11:30</th><th scope="col">-----</th><th scope="col">11:30 to 12:00</th><th scope="col">16:00 to 17:30</th><th scope="col">17:30 to 19:00</th><th scope="col">19:00 to 20:30</th><th scope="col">20:30 to 22:00</th>
		</tr><tr class="GridItem" style="border-width:1px;border-style:solid;height:70px;">
			<td>Monday</td><td> </td><td> </td><td>Data Structures<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z214(M)</asp:Label> <br/><font color=blue><b> Qurat Ul Ain 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2039</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60841</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >2766</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td>
		</tr><tr class="GridAlternatingItem" valign="middle" style="border-width:1px;border-style:solid;height:70px;">
			<td>Tuesday</td><td> </td><td> </td><td>Database Systems<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z205(M)</asp:Label> <br/><font color=blue><b> Atiqa Malik 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2638</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60844</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1838</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td>Fundamentals of Digital Logic Design<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z208(M)</asp:Label> <br/><font color=blue><b> Rabia Sajjad 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2001</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60850</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1841</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td> </td><td> </td><td> </td>
		</tr><tr class="GridItem" style="border-width:1px;border-style:solid;height:70px;">
			<td>Wednesday</td><td>Calculus and Analytic Geometry<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z214(M)</asp:Label> <br/><font color=blue><b> Dr. Saeed Ullah Jan 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >1253</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60853</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1829</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td>Database Systems<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z103(M)</asp:Label> <br/><font color=blue><b> Atiqa Malik 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2638</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60844</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1837</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td>Software Engineering<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z109(M)</asp:Label> <br/><font color=blue><b> Syed Shahab Zarin 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2480</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60847</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1844</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td> </td><td> </td><td> </td>
		</tr><tr class="GridAlternatingItem" valign="middle" style="border-width:1px;border-style:solid;height:70px;">
			<td>Thursday</td><td> </td><td>Calculus and Analytic Geometry<br/> <asp:Label 
 								ID="lblRoom" runat="server" >A120(60M)</asp:Label> <br/><font color=blue><b> Dr. Saeed Ullah Jan 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >1253</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60853</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1830</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td>Data Structures<br/> <asp:Label 
 								ID="lblRoom" runat="server" >Z220(M)</asp:Label> <br/><font color=blue><b> Qurat Ul Ain 
 								</b> </font> <asp:Label style="display:none;" 
 								ID="lblTeacherID" runat="server" >2039</asp:Label> <asp:Label style="display:none;" 
 								ID="lblOCID" runat="server" >60841</asp:Label>  <asp:Label style="display:none;" 
 								ID="lblMerger" runat="server" >1</asp:Label> <asp:Label style="display:none;" 
 								ID="lblID" runat="server" >1833</asp:Label> <asp:Label style="display:none;" 
 								ID="lblSchemeID" runat="server" >1327</asp:Label></td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td>
		</tr><tr class="GridItem" style="border-width:1px;border-style:solid;height:70px;">
			<td>Friday</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td>
		</tr><tr class="GridAlternatingItem" valign="middle" style="border-width:1px;border-style:solid;height:70px;">
			<td>Saturday</td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td><td> </td>
		</tr>
	</table>
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
