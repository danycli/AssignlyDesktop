

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
    <form method="post" action="./Summary.aspx" id="MasterForm">
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

	<input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="BCFEF455" />
	<input type="hidden" name="__PREVIOUSPAGE" id="__PREVIOUSPAGE" value="CzfvtEDHDcqAE6TsqVXiWLjUgrNaYTiSa9xT73_KMueKIkDvp8j-BUYhffX580LqUXnQgiMj3i89zMZSIAvxIK0whRCCyDsJh5S3g8WgMVg1" />
	<input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEdACpmOx+PFUbcV8qOvilM3Zezt117de9GXZItS/j0v5c8FAMAAazT+/WMwZRQd71Lh46ugUTu1xeqYaRkm2Q5sR6iRhZ09OUrkfKhyha3KQpix3Ghmbcm8utIcFjcCf3SrTj2ZuwxwA2ZhIUoPU+DCjPxW3REJ/F07wGnNgodfQlQb5F5pKCRCrdGHd7EG/oEndV1I2CyfDO5H6x+WIM7Zpvom18ay5/kywJJUeFNU2t9hYpQ1iWH3xqwmUEGZ6zrDenDvQp+7X3YiUVD4dzSk8ziuwamUWN9+dlFPzVGUVPc+hi7DHwV9jSlTxJ8/Tw9xETLZSFlVVfiO83Ng6WPYkZnfdW7s7nH9JqmDbFa0I8jJKPehRK32mASjyTKlFs2n94xq4iLQrDpP2vxU2q2qBIwEf2XmFW/yVE/iymIm/jHUEUHgUG2my0PvfAJFMftpMsHqV8iLps8pOhxwkLRFBdxDjbVml4GmYejckKb4NA7YIWR5uZhEb9nzG6GnYt/3wgtc9ISIVGDqQFC+isLkdBX7PtoyH61LMi2U9XM42C0LJ/WDIR1xLO01LhuglnD53+nvcpWSw+gMJZ3R3q4h8K8F6UQWycjksUaKTmZkqVRljyJapyatGT2ooFbtXJAFyO/W+xz0RuvqSqtMg7lDgrB3815PnzFyKljmjOYAsKBIjmnOFqdsvsy9zCA9ifXICKkrsi//chuiHLeY4WhbrdsMqh75Eit/SGEIWJSyFqaPzFYhQaIXF8QI08pBIfMVEqWDi7S3AAfGebovq+o7xzDT/hEPdEVSb402Uc5msFp34G8YXVCDGHS5M9OHmJen5SNhlPteG2QbHiU5DI/oYmXSBlqNEY7A5tYYdAaBmMZmxzkh6KAvyeV7qpOP+G3jJjyJ7nJbE3mSyJIVVIWCfWc1PZEhoBdoq7x0rzuX/4h8w==" />
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
                            
                            <li id="lstCourses"><a id="lnk_Courses" class="current nav-top-item">
                                <!-- Add the class "current" to current menu item -->
                                Courses </a>
                                <ul>
                                    <li>
                                        <a id="lnk_Summary" class="current" href="javascript:__doPostBack(&#39;ctl00$lnk_Summary&#39;,&#39;&#39;)">Summary</a></li>
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
                            
    <h3>
        Student's Courses Summary</h3>

                            <div class="clear">
                            </div>
                        </div>
                        

    <script type="text/jscript">

        var color = true;

        var int = setInterval('blink()', 500);

        function blink() {
            if (document.getElementById('ctl00_DataContent_linkCourseReg') != null) {
                if (color == true) {
                    document.getElementById('ctl00_DataContent_linkCourseReg').style.color = "#0f0";
                    color = false;
                }
                else {
                    document.getElementById('ctl00_DataContent_linkCourseReg').style.color = "#f0f";
                    color = true;
                }
            }
        }


        function stopBlink() {
            clearInterval(int);
        }
    </script>

    <table width="100%">
        <tr>
            <td width="15%">
            </td>
            <td width="70%">
                <div id="DataContent_dvMessage" style="text-align: left; width:70%;" class="notification information png_bg">
                    <a href="#" class="close">
                        <img src='/resources/images/icons/cross_grey_small.png' title="Close this notification"
                            alt="close" onclick="CloseNotification();" /></a>
                    <div>
                        <ol><li>Click Course Title to Select Course</li><li>Click attendance percentage to view attendance details</li></ol>
                    </div>
                </div>
            </td>
            <td width="15%">
            </td>
        </tr>
    </table>
    
    
    
    <br />
    <div>
	<table class="Grid" cellspacing="0" rules="all" id="DataContent_gvCourseSummary" style="border-color:#D8D8D8;border-width:1px;border-style:solid;width:97%;border-collapse:collapse;">
		<tr>
			<th class="GridHeader" scope="col">S#</th><th class="GridHeader" scope="col">Course Title</th><th class="GridHeader" scope="col">Class</th><th class="GridHeader" scope="col">Faculty</th><th class="GridHeader" scope="col">Lectures</th><th class="GridHeader" scope="col">P</th><th class="GridHeader" scope="col">A</th><th class="GridHeader" scope="col">Thy%</th><th class="GridHeader" scope="col">LAB%</th>
		</tr><tr class="GridItem">
			<td class="GridItem">
                    <span id="DataContent_gvCourseSummary_lblSRNO_0">1</span>
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lbCourse_0" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl02$lbCourse&#39;,&#39;&#39;)">Calculus and Analytic Geometry</a>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbClass_0">BCS 3 C                                                 </span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbFaculty_0">Dr. Saeed Ullah Jan</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbTotal_0">16</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbPresent_0">15</span>
                </td><td class="GridColumn">
                    
                    
                    
                    
                    <span id="DataContent_gvCourseSummary_lbAbsent_0">1</span>
                </td><td class="GridColumn">
                       <a id="DataContent_gvCourseSummary_lnkThAttendance_0" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl02$lnkThAttendance&#39;,&#39;&#39;)">94%</a>
                
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lnkLabAttendance_0" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl02$lnkLabAttendance&#39;,&#39;&#39;)">N/A</a>
                  
                </td>
		</tr><tr class="GridAlternatingItem">
			<td class="GridItem">
                    <span id="DataContent_gvCourseSummary_lblSRNO_1">2</span>
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lbCourse_1" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl03$lbCourse&#39;,&#39;&#39;)">Data Structures</a>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbClass_1">BCS 3 C</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbFaculty_1">Qurat Ul Ain</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbTotal_1">27</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbPresent_1">22</span>
                </td><td class="GridColumn">
                    
                    
                    
                    
                    <span id="DataContent_gvCourseSummary_lbAbsent_1">5</span>
                </td><td class="GridColumn">
                       <a id="DataContent_gvCourseSummary_lnkThAttendance_1" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl03$lnkThAttendance&#39;,&#39;&#39;)">94%</a>
                
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lnkLabAttendance_1" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl03$lnkLabAttendance&#39;,&#39;&#39;)">64%</a>
                  
                </td>
		</tr><tr class="GridItem">
			<td class="GridItem">
                    <span id="DataContent_gvCourseSummary_lblSRNO_2">3</span>
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lbCourse_2" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl04$lbCourse&#39;,&#39;&#39;)">Database Systems</a>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbClass_2">BCS 3 C                                                 </span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbFaculty_2">Atiqa Malik</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbTotal_2">28</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbPresent_2">22</span>
                </td><td class="GridColumn">
                    
                    
                    
                    
                    <span id="DataContent_gvCourseSummary_lbAbsent_2">6</span>
                </td><td class="GridColumn">
                       <a id="DataContent_gvCourseSummary_lnkThAttendance_2" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl04$lnkThAttendance&#39;,&#39;&#39;)">86%</a>
                
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lnkLabAttendance_2" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl04$lnkLabAttendance&#39;,&#39;&#39;)">71%</a>
                  
                </td>
		</tr><tr class="GridAlternatingItem">
			<td class="GridItem">
                    <span id="DataContent_gvCourseSummary_lblSRNO_3">4</span>
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lbCourse_3" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl05$lbCourse&#39;,&#39;&#39;)">Fundamentals of Digital Logic Design</a>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbClass_3">BCS 3 C                                                 </span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbFaculty_3">Rabia Sajjad</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbTotal_3">29</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbPresent_3">25</span>
                </td><td class="GridColumn">
                    
                    
                    
                    
                    <span id="DataContent_gvCourseSummary_lbAbsent_3">4</span>
                </td><td class="GridColumn">
                       <a id="DataContent_gvCourseSummary_lnkThAttendance_3" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl05$lnkThAttendance&#39;,&#39;&#39;)">86%</a>
                
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lnkLabAttendance_3" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl05$lnkLabAttendance&#39;,&#39;&#39;)">87%</a>
                  
                </td>
		</tr><tr class="GridItem">
			<td class="GridItem">
                    <span id="DataContent_gvCourseSummary_lblSRNO_4">5</span>
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lbCourse_4" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl06$lbCourse&#39;,&#39;&#39;)">Software Engineering</a>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbClass_4">BCS 3 C</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbFaculty_4">Syed Shahab Zarin</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbTotal_4">13</span>
                </td><td class="GridColumn">
                    <span id="DataContent_gvCourseSummary_lbPresent_4">12</span>
                </td><td class="GridColumn">
                    
                    
                    
                    
                    <span id="DataContent_gvCourseSummary_lbAbsent_4">1</span>
                </td><td class="GridColumn">
                       <a id="DataContent_gvCourseSummary_lnkThAttendance_4" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl06$lnkThAttendance&#39;,&#39;&#39;)">92%</a>
                
                </td><td class="GridColumn">
                    <a id="DataContent_gvCourseSummary_lnkLabAttendance_4" href="javascript:__doPostBack(&#39;ctl00$DataContent$gvCourseSummary$ctl06$lnkLabAttendance&#39;,&#39;&#39;)">N/A</a>
                  
                </td>
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
