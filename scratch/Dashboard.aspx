

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
    <form method="post" action="./Dashboard.aspx" id="MasterForm">
<div class="aspNetHidden">
<input type="hidden" name="__EVENTTARGET" id="__EVENTTARGET" value="" />
<input type="hidden" name="__EVENTARGUMENT" id="__EVENTARGUMENT" value="" />
<input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDwULLTEyNjA3OTMwMzQPZBYCZg9kFgJmD2QWBAIBD2QWAgIDD2QWAgIBDzwrABECARAWABYAFgAMFCsAAGQCCw9kFh4CAw8PFgIeBFRleHQFDERhbmlhbCBBaG1lZGRkAggPD2QWAh4FU3R5bGUFDWRpc3BsYXk6bm9uZTtkAgkPDxYEHghDc3NDbGFzcwUUY3VycmVudCBuYXYtdG9wLWl0ZW0eBF8hU0ICAmRkAgsPZBYEZg8WAh4Fc3R5bGUFDWRpc3BsYXk6bm9uZTtkAgIPD2QWAh8EBQ1kaXNwbGF5Om5vbmU7ZAIMDxYCHwQFDGRpc3BsYXk6bm9uZWQCFA8WAh8EBQ1kaXNwbGF5Om5vbmU7ZAIVDxYCHwQFDWRpc3BsYXk6bm9uZTtkAhYPFgIfBAUNZGlzcGxheTpub25lO2QCFw8WAh8EBQ1kaXNwbGF5Om5vbmU7ZAIjD2QWBGYPFgIfBAUMZGlzcGxheTpub25lZAIGDxYCHwQFDGRpc3BsYXk6bm9uZWQCPA8WAh4HVmlzaWJsZWhkAj8PZBYEAgIPD2QWAh8EBQ1kaXNwbGF5Om5vbmU7ZAIEDw9kFgIfBAUNZGlzcGxheTpub25lO2QCRw9kFhgCAQ8WAh4Dc3JjBTBQaWN0dXJlSGFuZGxlci5hc2h4P3JlZ19ubz1DSUlUL1NQMjUtQkNTLTEzNi9BVERkAgMPDxYCHwAFDERhbmlhbCBBaG1lZGRkAgUPDxYCHwAFFUNJSVQvU1AyNS1CQ1MtMTM2L0FURGRkAgcPDxYCHwAFGFNhaGliemFkYSBNdWhhbW1hZCBJc2hhcWRkAgkPDxYCHwAFATVkZAINDw8WAh8ABQIxNmRkAhEPDxYCHwAFA0JDU2RkAhMPDxYCHwAFAkMgZGQCFQ8PFgIfAAUCTkFkZAIXDw8WAh8ABQxGZWIgMTIsIDIwMDZkZAIZDw8WAh8ABQ8xNjIwMi0yMDA3MTQyLTdkZAIbDw8WAh8ABQJOQWRkAkkPZBYIAgMPFgIfBWcWAmYPZBYCAgEPPCsAEQMADxYEHgtfIURhdGFCb3VuZGceC18hSXRlbUNvdW50AgFkARAWABYAFgAMFCsAABYCZg9kFgQCAQ9kFgxmD2QWAmYPFQEBMWQCAQ9kFgICAQ8PFgIfAAUcSFNTQyBDZXJ0aWZpY2F0ZSAoU2FuYWQpIC0gRGRkAgIPZBYCAgEPDxYCHwAFAk5BZGQCAw9kFgICAQ8PFgIfAAUNTm90IFJlY292ZXJlZGRkAgQPZBYCAgEPDxYCHwBlZGQCBQ9kFgICAQ8PFgIfAAUFRmFsc2VkZAICDw8WAh8FaGRkAgUPPCsAEQMADxYEHwdnHwgCBWQBEBYAFgAWAAwUKwAAFgJmD2QWDAIBD2QWDmYPZBYCZg8VAQExZAIBD2QWAmYPFQEPRGF0YSBTdHJ1Y3R1cmVzZAICD2QWAmYPFQEMUXVyYXQgVWwgQWluZAIDDw8WAh8ABQxKdW4gMDEsIDIwMjZkZAIED2QWAmYPFQEFMTI6MDBkAgUPZBYCZg8VAQUgQTIyOGQCBg9kFgJmDxUBAzI2MGQCAg9kFg5mD2QWAmYPFQEBMmQCAQ9kFgJmDxUBHkNhbGN1bHVzIGFuZCBBbmFseXRpYyBHZW9tZXRyeWQCAg9kFgJmDxUBE0RyLiBTYWVlZCBVbGxhaCBKYW5kAgMPDxYCHwAFDEp1biAwMiwgMjAyNmRkAgQPZBYCZg8VAQUwODozMGQCBQ9kFgJmDxUBBSBIUkE0ZAIGD2QWAmYPFQEDNTI3ZAIDD2QWDmYPZBYCZg8VAQEzZAIBD2QWAmYPFQEQRGF0YWJhc2UgU3lzdGVtc2QCAg9kFgJmDxUBC0F0aXFhIE1hbGlrZAIDDw8WAh8ABQxKdW4gMDMsIDIwMjZkZAIED2QWAmYPFQEFMTc6MTVkAgUPZBYCZg8VAQUgSFJBNGQCBg9kFgJmDxUBAzQ4MmQCBA9kFg5mD2QWAmYPFQEBNGQCAQ9kFgJmDxUBJEZ1bmRhbWVudGFscyBvZiBEaWdpdGFsIExvZ2ljIERlc2lnbmQCAg9kFgJmDxUBDFJhYmlhIFNhamphZGQCAw8PFgIfAAUMSnVuIDA0LCAyMDI2ZGQCBA9kFgJmDxUBBTA4OjMwZAIFD2QWAmYPFQEFIEhSQTNkAgYPZBYCZg8VAQMzMjNkAgUPZBYOZg9kFgJmDxUBATVkAgEPZBYCZg8VARRTb2Z0d2FyZSBFbmdpbmVlcmluZ2QCAg9kFgJmDxUBEVN5ZWQgU2hhaGFiIFphcmluZAIDDw8WAh8ABQxKdW4gMDUsIDIwMjZkZAIED2QWAmYPFQEFMTA6MTVkAgUPZBYCZg8VAQUgSFJBMmQCBg9kFgJmDxUBAzE2MGQCBg8PFgIfBWhkZAIHDxYCHwVoFgICAQ88KwARAwAPFgQfB2cfCGZkARAWABYAFgAMFCsAAGQCEQ8PFgIfBWhkZAJKDw8WAh8ABQpBYmJvdHRhYmFkZGQYBAUhY3RsMDAkRGF0YUNvbnRlbnQkZ3ZOb3RpZmljYXRpb25zDzwrAAwBCGZkBR1jdGwwMCREYXRhQ29udGVudCRndkRhdGVTaGVldA88KwAMAQgCAWQFJGN0bDAwJERhdGFDb250ZW50JGd2TWlzc2luZ0RvY3VtZW50cw88KwAMAQgCAWQFGGN0bDAwJGd2TWlzc2luZ0RvY3VtZW50cw9nZLG+bLsn8dvHCoa0YquAThbCp8WMBRB6BzG7vqIi8WCx" />
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

	<input type="hidden" name="__VIEWSTATEGENERATOR" id="__VIEWSTATEGENERATOR" value="B543B226" />
	<input type="hidden" name="__PREVIOUSPAGE" id="__PREVIOUSPAGE" value="nyLCx6oPVN-FxI3DWG-f1JG-P7kKWcioZxMHVrRJq_o43PArKs814QCnBkkFpPb-qv5FeFpkPhbbThNhzzrVUzo_OjqSRrNbS_xpVQNKK8g1" />
	<input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEdABsqq+pA9KIKOgKxkA9WGDvSt117de9GXZItS/j0v5c8FAMAAazT+/WMwZRQd71Lh46ugUTu1xeqYaRkm2Q5sR6iRhZ09OUrkfKhyha3KQpix3Ghmbcm8utIcFjcCf3SrTj2ZuwxwA2ZhIUoPU+DCjPxW3REJ/F07wGnNgodfQlQb5F5pKCRCrdGHd7EG/oEndV1I2CyfDO5H6x+WIM7Zpvom18ay5/kywJJUeFNU2t9hYpQ1iWH3xqwmUEGZ6zrDenDvQp+7X3YiUVD4dzSk8ziuwamUWN9+dlFPzVGUVPc+hi7DHwV9jSlTxJ8/Tw9xETLZSFlVVfiO83Ng6WPYkZnfdW7s7nH9JqmDbFa0I8jJKPehRK32mASjyTKlFs2n94xq4iLQrDpP2vxU2q2qBIwEf2XmFW/yVE/iymIm/jHUEUHgUG2my0PvfAJFMftpMsHqV8iLps8pOhxwkLRFBdxDjbVml4GmYejckKb4NA7YIWR5uZhEb9nzG6GnYt/3wgtc9ISIVGDqQFC+isLkdBX7PtoyH61LMi2U9XM42C0LJ/WDIR1xLO01LhuglnD53/rnTsEvsHNxJbfaSfp18Vafw98ku0yiCUEkcixURgbAg==" />
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
                                <a id="lnk_Dashboard" class="current nav-top-item" href="javascript:__doPostBack(&#39;ctl00$lnk_Dashboard&#39;,&#39;&#39;)">Dashboard</a>
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
                            
    <h3>Dash Board</h3>
    <head>

        <style>
            .exam-notice {
                max-width: 800px;
                margin: 30px auto;
                padding: 20px 25px;
                border: 2px solid #000;
                font-family: Arial, Helvetica, sans-serif;
                line-height: 1.6;
                background-color: #fff;
            }

                .exam-notice h2 {
                    text-align: center;
                    color: #c00000;
                    margin-bottom: 15px;
                    text-transform: uppercase;
                }

                .exam-notice p {
                    margin-bottom: 12px;
                    font-size: 15px;
                    color: #000;
                    text-align: justify;
                }

                .exam-notice strong {
                    font-weight: bold;
                }
        </style>




        <!--POP UP Starts here -->
        <link rel="stylesheet" href="colorbox.css" />

        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>

        <script src="jquery.colorbox.js"></script>

        <script>
            $(document).ready(function () {

                var showDocs = true;//$("#DataContent_gvMissingDocuments tr").length > 1;
                var showDateSheet = $("#DataContent_gvDateSheet tr").length > 1;
               function showWinterPopup() {
                    $.colorbox({
                        width: "90%",
                        inline: true,
                        href: "#winterpopup"
                    });
                }
                function showDateSheetPopup() {
                    $.colorbox({
                        width: "90%",
                        inline: true,
                        href: "#popup",
                        onClosed: function () {

                            showWinterPopup();
                        }
                    });
                }
                if (showDocs && showDateSheet) {
                    // Show first popup
                     
                    $.colorbox({
                        width: "90%",
                        inline: true,
                        href: "#docspopup",
                        onClosed: function () {
                            // Show second popup after closing first
                            showDateSheetPopup();

                        }
                    });
                }
                else if (showDocs) {
                    $.colorbox({
                        width: "90%",
                        inline: true,
                        href: "#docspopup"
                    });
                }
                else if (showDateSheet) {
                    showDateSheetPopup();
                }
               

            });
        </script>




    </head>

                            <div class="clear">
                            </div>
                        </div>
                        

    <script>

        var color = true

        var int = setInterval('blink()', 500)

        function blink() {
            if (color == true) {
                document.getElementById('link').style.color = "#0f0"
                color = false
            }
            else {
                document.getElementById('link').style.color = "#f0f"
                color = true
            }
        }


        function stopBlink() {
            clearInterval(int)
        }
    </script>
    <!--  <div style="display: none;">
      <div id='winterpopup' style='padding: 10px; background: #fff;'>
            <h2>Winter Semester</h2>
            <h4>
            COMSATS University Islamabad Campus has started the Winter Semester. Students can express interest in registering for:
            <ol>
                <li>Failed courses</li>

                <li>Withdrawn courses</li>

                <li>Courses for improvement (only if the previous grade was D)</li>

            </ol>
            <b>Important:</b> This is only to show your interest. Actual registration will be available based on the courses offered in the Winter Semester. It is important to indicate your interest, as course offerings will depend on student demand.
            <br />
            Please submit your interest if you plan to register for any course in the Winter Semester.
       <a href="CoursesForWinterSemester.aspx" target="_blank">Click Here To Submit Your Interest</a>
      </h4>  </div>
    </div>-->
    <div style="display: none;">
        <div id='docspopup' style='padding: 10px; background: #fff;'>
 
            <div id="DataContent_missingdocsDiv" class="notification information png_bg" style="width: 95%;">
                <a href="#" class="close">
                    <img src='/resources/images/icons/cross_grey_small.png' title="Close this notification"
                        alt="close" onclick="CloseNotification();" /></a>
                <div id="DataContent_lbMissingdocs">
                    <div style="text-align: center;">
                        <b>Missing Documents Details</b>
                    </div>
                    <div>
	<table cellspacing="0" rules="all" id="DataContent_gvMissingDocuments" style="background-color:White;border-color:#D8D8D8;border-width:1px;border-style:solid;width:100%;border-collapse:collapse;">
		<tr style="color:White;background-color:#3980F4;">
			<th scope="col">S.no</th><th scope="col">Missing Doc.</th><th scope="col">Missing Reason</th><th scope="col">Doc Status.</th><th scope="col">Recovery Date</th>
		</tr><tr style="font-size:11px;">
			<td>
                                    1
                                </td><td>
                                    <span id="DataContent_gvMissingDocuments_lbMissingDoc_0">HSSC Certificate (Sanad) - D</span>
                                </td><td>
                                    <span id="DataContent_gvMissingDocuments_lbMissingReason_0">NA</span>
                                </td><td>
                                    <span id="DataContent_gvMissingDocuments_lbStatus_0">Not Recovered</span>
                                </td><td>
                                    <span id="DataContent_gvMissingDocuments_lbRecoveryDate_0"></span>
                                </td>
		</tr>
	</table>
</div>
                </div>
            </div>
        </div>
    </div>

    <div style="display: none;">
        <div id='popup' style='padding: 10px; background: #fff;'>


            <h2>Important Notice for Students – Exams SP26</h2>

            <p>
                <strong>Dear Student,</strong> possession of mobile phones, smart watches,
        electronic devices, or any kind of helping material is strictly prohibited
        in the examination hall / room.
     <br />
                If found with any student, whether used or not used for cheating, it will
        lead to an <strong>Unfair Means case</strong>.
    
     <br />
                Mobile phones, smart watches, and electronic devices will be confiscated,
        and the University will not be responsible for any damage to these devices
        during confiscation.
      <br />
                Students are strongly advised <strong>not to bring</strong> any of the above
        devices while coming to the examination hall / room.
		<br />
		If your examination is scheduled in the <strong>Haroon Rashid Auditorium (HRA)</strong>, please bring the <strong>clipboard</strong> with you.
            </p>

            
            <h2 style="color: blue;">Date Sheet Midtrem 
            </h2>
            <br />
            <div>
	<table class="Grid" cellspacing="0" rules="all" id="DataContent_gvDateSheet" style="border-color:#D8D8D8;border-width:1px;border-style:solid;width:97%;border-collapse:collapse;">
		<tr>
			<th class="GridHeader" scope="col">S#</th><th class="GridHeader" scope="col">Course Title</th><th class="GridHeader" scope="col">Faculty</th><th class="GridHeader" scope="col">Date</th><th class="GridHeader" scope="col">Time</th><th class="GridHeader" scope="col">Venue</th><th class="GridHeader" scope="col">Seat No</th>
		</tr><tr class="GridItem">
			<td class="GridItem">
                            1
                        </td><td class="GridColumn">
                            Data Structures
                        </td><td class="GridColumn">
                            Qurat Ul Ain
                        </td><td class="GridColumn" style="width:10%;">Jun 01, 2026</td><td class="GridColumn">
                            12:00
                        </td><td class="GridColumn">
                             A228
                        </td><td class="GridColumn">
                            260
                        </td>
		</tr><tr class="GridAlternatingItem">
			<td class="GridItem">
                            2
                        </td><td class="GridColumn">
                            Calculus and Analytic Geometry
                        </td><td class="GridColumn">
                            Dr. Saeed Ullah Jan
                        </td><td class="GridColumn" style="width:10%;">Jun 02, 2026</td><td class="GridColumn">
                            08:30
                        </td><td class="GridColumn">
                             HRA4
                        </td><td class="GridColumn">
                            527
                        </td>
		</tr><tr class="GridItem">
			<td class="GridItem">
                            3
                        </td><td class="GridColumn">
                            Database Systems
                        </td><td class="GridColumn">
                            Atiqa Malik
                        </td><td class="GridColumn" style="width:10%;">Jun 03, 2026</td><td class="GridColumn">
                            17:15
                        </td><td class="GridColumn">
                             HRA4
                        </td><td class="GridColumn">
                            482
                        </td>
		</tr><tr class="GridAlternatingItem">
			<td class="GridItem">
                            4
                        </td><td class="GridColumn">
                            Fundamentals of Digital Logic Design
                        </td><td class="GridColumn">
                            Rabia Sajjad
                        </td><td class="GridColumn" style="width:10%;">Jun 04, 2026</td><td class="GridColumn">
                            08:30
                        </td><td class="GridColumn">
                             HRA3
                        </td><td class="GridColumn">
                            323
                        </td>
		</tr><tr class="GridItem">
			<td class="GridItem">
                            5
                        </td><td class="GridColumn">
                            Software Engineering
                        </td><td class="GridColumn">
                            Syed Shahab Zarin
                        </td><td class="GridColumn" style="width:10%;">Jun 05, 2026</td><td class="GridColumn">
                            10:15
                        </td><td class="GridColumn">
                             HRA2
                        </td><td class="GridColumn">
                            160
                        </td>
		</tr>
	</table>
</div>
            <br />

        </div>
    </div>

    <div>
        
    </div>
    <br />
    
    
    <br />

    

    <br />
   
    <br />
   <!-- <a href="CoursesForWinterSemester.aspx" target="_blank" visible="false" class="button">
      <h2 style="color:#fff">  Click Here For Winter Semester </h2></a> -->
   <br />
   
    <br />
    <div style="overflow: auto; text-align: center; padding-left: 30px;">
        <div id="attendanceChart" style="height: 300px; width: 96%;"></div>
        <br />
        <div id="courseAttendanceChart" style="height: 300px; width: 96%;"></div>
        <br />
         <div id="resultGraph" style="height: 300px; width: 96%;"></div>
        <br />
      <br />
        <div id="gpBarChart" style="height: 300px; width: 96%;"></div>
    </div>
    <div style="overflow: auto;">
        
    </div>
    <div id="DataContent_dvLab" style="overflow: auto;">
        
    </div>
    <br />
    <br />
    <div style="overflow: auto">
        
    </div>
    <br />
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
    
<script>  var dom = document.getElementById('attendanceChart');
                 var myChart = echarts.init(dom, 'dark', 
                { renderer: 'canvas', useDirtyRect: true });
                var app = {};
                var option;
                const posListattendanceChart = [
                'left',  'right',  'top',  'bottom',  'inside',  'insideTop',  'insideLeft',  'insideRight',
                'insideBottom',  'insideTopLeft',  'insideTopRight',  'insideBottomLeft',  'insideBottomRight'
                ];
                app.configParameters = {rotate: { min: -90,max: 90 },
                align: {options: 
                    {left: 'left', center: 'center', right: 'right' }  },
                verticalAlign: { 
                    options: {top: 'top', middle: 'middle', bottom: 'bottom'}},
                position: { options: posListattendanceChart.reduce(function (map, pos) { map[pos] = pos; return map; }, {}) },
                distance: {    min: 0,    max: 100  }};
                app.config = {  rotate: 90,  align: 'left',  
                verticalAlign: 'middle',  position: 'insideBottom',
                distance: 15,  
                onChange: function () 
                    { const labelOption = { rotate: app.config.rotate,
                        align: app.config.align, verticalAlign: app.config.verticalAlign, position: app.config.position, distance: app.config.distance };
                        myChart.setOption({ 
                        series: [ { label: labelOption },{label: labelOption },
                        { label: labelOption}, { label: labelOption } ] }); }};

                const labelOption = {  show: true,  position: app.config.position, distance: app.config.distance, align: app.config.align, verticalAlign: app.config.verticalAlign,  rotate: app.config.rotate, formatter: '{c} ',  fontSize: 13 };
                option = { 
                title: {
                    text: 'Course Attendance Percentage', // The main title
                    subtext: 'Month Based',       // Optional subtitle
                    left: 'center',                 // Position: 'left', 'center', 'right'
                    top: 'top',                     // Position from top
                    textStyle: {                    // Title style
                        color: '#fff',             // For dark theme
                        fontSize: 16,
                        fontWeight: 'bold'
                    },
                    subtextStyle: {
                        color: '#ccc',
                        fontSize: 12
                    }
                },
              tooltip: {
                        trigger: 'axis',
                        axisPointer: { type: 'shadow' },
                        formatter: function (params) {

                            // KEEP only bars that actually exist
                            params = params.filter(p =>
                                p.data !== null && p.data !== undefined
                            );

                            if (params.length === 0)
                                return '';

                            let html = params[0].axisValue + '<br/>';

                            params.forEach(p => {
                                html +=
                                    p.marker +
                                    p.seriesName +
                                    ': ' +
                                    p.data +
                                    '<br/>';
                            });

                            return html;
                        }
                    }
,
                legend: {show: true, orient: 'horizontal', left: 'center',   top: 40,  data: ['CSC211','CSC270','CSC291','EEE240','MTH104'] },
                toolbox: { show: true, orient: 'vertical', left: 'right', top: 'center',
                feature: { mark: { show: true }, dataView: { show: true, readOnly: false },
                magicType: { show: true, type: ['line', 'bar', 'stack'] }, restore: { show: true },
                saveAsImage: { show: true } } },
                xAxis: [ { type: 'category', axisTick: { show: true }, 
                data: ['March','April','May'],axisLabel: { interval: 0, rotate: 45 } } ],
                yAxis: [ { type: 'value'  } ],
                series: [
            {
                name: 'CSC211',
                type: 'bar',
                barGap: 0,
                label: labelOption,
                emphasis: { focus: 'series' },
                data: [100,70,100]
            },
            {
                name: 'CSC270',
                type: 'bar',
                barGap: 0,
                label: labelOption,
                emphasis: { focus: 'series' },
                data: [100,76,75]
            },
            {
                name: 'CSC291',
                type: 'bar',
                barGap: 0,
                label: labelOption,
                emphasis: { focus: 'series' },
                data: [0,100,80]
            },
            {
                name: 'EEE240',
                type: 'bar',
                barGap: 0,
                label: labelOption,
                emphasis: { focus: 'series' },
                data: [100,75,100]
            },
            {
                name: 'MTH104',
                type: 'bar',
                barGap: 0,
                label: labelOption,
                emphasis: { focus: 'series' },
                data: [0,100,83]
            }      ] }; 
                if (option && typeof option === 'object') {
                myChart.setOption(option);
                }
                   window.addEventListener('resize', myChart.resize); </script>
(function(){
    <script>  var dom = document.getElementById('courseAttendanceChart');
                 var myChart = echarts.init(dom, 'dark', 
                { renderer: 'canvas', useDirtyRect: true });
                var app = {};
                var option;
                const posListcourseAttendanceChart = [
                'left',  'right',  'top',  'bottom',  'inside',  'insideTop',  'insideLeft',  'insideRight',
                'insideBottom',  'insideTopLeft',  'insideTopRight',  'insideBottomLeft',  'insideBottomRight'
                ];
                app.configParameters = {rotate: { min: -90,max: 90 },
                align: {options: 
                    {left: 'left', center: 'center', right: 'right' }  },
                verticalAlign: { 
                    options: {top: 'top', middle: 'middle', bottom: 'bottom'}},
                position: { options: posListcourseAttendanceChart.reduce(function (map, pos) { map[pos] = pos; return map; }, {}) },
                distance: {    min: 0,    max: 100  }};
                app.config = {  rotate: 90,  align: 'left',  
                verticalAlign: 'middle',  position: 'insideBottom',
                distance: 15,  
                onChange: function () 
                    { const labelOption = { rotate: app.config.rotate,
                        align: app.config.align, verticalAlign: app.config.verticalAlign, position: app.config.position, distance: app.config.distance };
                        myChart.setOption({ 
                        series: [ { label: labelOption },{label: labelOption },
                        { label: labelOption}, { label: labelOption } ] }); }};

                
                option = { 
                title: {
                    text: 'Course Attendance Percentage', // The main title
                    subtext: 'Overall',       // Optional subtitle
                    left: 'center',                 // Position: 'left', 'center', 'right'
                    top: 'top',                     // Position from top
                    textStyle: {                    // Title style
                        color: '#fff',             // For dark theme
                        fontSize: 16,
                        fontWeight: 'bold'
                    },
                    subtextStyle: {
                        color: '#ccc',
                        fontSize: 12
                    }
                },
              tooltip: {
                        trigger: 'axis',
                        axisPointer: { type: 'shadow' },
                        formatter: function (params) {

                            // KEEP only bars that actually exist
                            params = params.filter(p =>
                                p.data !== null && p.data !== undefined
                            );

                            if (params.length === 0)
                                return '';

                            let html = params[0].axisValue + '<br/>';

                            params.forEach(p => {
                                html +=
                                    p.marker +
                                    p.seriesName +
                                    ': ' +
                                    p.data +
                                    '<br/>';
                            });

                            return html;
                        }
                    }
,
                legend: {show: true, orient: 'horizontal', left: 'center',   top: 40,  data: ['CSC211          ','CSC270          ','CSC291          ','EEE240          ','MTH104          '] },
                toolbox: { show: true, orient: 'vertical', left: 'right', top: 'center',
                feature: { mark: { show: true }, dataView: { show: true, readOnly: false },
                magicType: { show: true, type: ['line', 'bar', 'stack'] }, restore: { show: true },
                saveAsImage: { show: true } } },
                xAxis: [ { type: 'category', axisTick: { show: true }, 
                data: ['CSC211          ','CSC270          ','CSC291          ','EEE240          ','MTH104          '],axisLabel: { interval: 0, rotate: 45 } } ],
                yAxis: [ { type: 'value'  } ],
                series: [
            {
                name: 'CSC211          ',
                type: 'bar',
                label: { show: true, position: 'top' },
                itemStyle: { color: '#5470C6' },
                emphasis: { focus: 'series' },
                data: [81.48,null,null,null,null]
            },
            {
                name: 'CSC270          ',
                type: 'bar',
                label: { show: true, position: 'top' },
                itemStyle: { color: '#91CC75' },
                emphasis: { focus: 'series' },
                data: [null,78.57,null,null,null]
            },
            {
                name: 'CSC291          ',
                type: 'bar',
                label: { show: true, position: 'top' },
                itemStyle: { color: '#EE6666' },
                emphasis: { focus: 'series' },
                data: [null,null,92.31,null,null]
            },
            {
                name: 'EEE240          ',
                type: 'bar',
                label: { show: true, position: 'top' },
                itemStyle: { color: '#73C0DE' },
                emphasis: { focus: 'series' },
                data: [null,null,null,86.21,null]
            },
            {
                name: 'MTH104          ',
                type: 'bar',
                label: { show: true, position: 'top' },
                itemStyle: { color: '#FAC858' },
                emphasis: { focus: 'series' },
                data: [null,null,null,null,93.75]
            }      ] }; 
                if (option && typeof option === 'object') {
                myChart.setOption(option);
                }
                   window.addEventListener('resize', myChart.resize); </script>
})();

                    (function() {
                        <script>  var dom = document.getElementById('gpBarChart');
                 var myChart = echarts.init(dom, 'dark', 
                { renderer: 'canvas', useDirtyRect: true });
                var app = {};
                var option;
                const posListgpBarChart = [
                'left',  'right',  'top',  'bottom',  'inside',  'insideTop',  'insideLeft',  'insideRight',
                'insideBottom',  'insideTopLeft',  'insideTopRight',  'insideBottomLeft',  'insideBottomRight'
                ];
                app.configParameters = {rotate: { min: -90,max: 90 },
                align: {options: 
                    {left: 'left', center: 'center', right: 'right' }  },
                verticalAlign: { 
                    options: {top: 'top', middle: 'middle', bottom: 'bottom'}},
                position: { options: posListgpBarChart.reduce(function (map, pos) { map[pos] = pos; return map; }, {}) },
                distance: {    min: 0,    max: 100  }};
                app.config = {  rotate: 90,  align: 'left',  
                verticalAlign: 'middle',  position: 'insideBottom',
                distance: 15,  
                onChange: function () 
                    { const labelOption = { rotate: app.config.rotate,
                        align: app.config.align, verticalAlign: app.config.verticalAlign, position: app.config.position, distance: app.config.distance };
                        myChart.setOption({ 
                        series: [ { label: labelOption },{label: labelOption },
                        { label: labelOption}, { label: labelOption } ] }); }};

                
                option = { 
                title: {
                    text: 'Student CGPA', // The main title
                    subtext: 'Semester Wise',       // Optional subtitle
                    left: 'center',                 // Position: 'left', 'center', 'right'
                    top: 'top',                     // Position from top
                    textStyle: {                    // Title style
                        color: '#fff',             // For dark theme
                        fontSize: 16,
                        fontWeight: 'bold'
                    },
                    subtextStyle: {
                        color: '#ccc',
                        fontSize: 12
                    }
                },
              tooltip: {
                        trigger: 'axis',
                        axisPointer: { type: 'shadow' },
                        formatter: function (params) {

                            // KEEP only bars that actually exist
                            params = params.filter(p =>
                                p.data !== null && p.data !== undefined
                            );

                            if (params.length === 0)
                                return '';

                            let html = params[0].axisValue + '<br/>';

                            params.forEach(p => {
                                html +=
                                    p.marker +
                                    p.seriesName +
                                    ': ' +
                                    p.data +
                                    '<br/>';
                            });

                            return html;
                        }
                    }
,
                legend: {show: true, orient: 'horizontal', left: 'center',   top: 40,  data: ['SP25','FA25','WS26'] },
                toolbox: { show: true, orient: 'vertical', left: 'right', top: 'center',
                feature: { mark: { show: true }, dataView: { show: true, readOnly: false },
                magicType: { show: true, type: ['line', 'bar', 'stack'] }, restore: { show: true },
                saveAsImage: { show: true } } },
                xAxis: [ { type: 'category', axisTick: { show: true }, 
                data: ['SP25','FA25','WS26'],axisLabel: { interval: 0, rotate: 45 } } ],
                yAxis: [ { type: 'value'  } ],
                series: [
        {
            name: 'CGPA',
            type: 'bar',
            barGap: 0,
            label: labelOption,
            emphasis: { focus: 'series' },
            data: [3.87,3.76,0]
        }      ] }; 
                if (option && typeof option === 'object') {
                myChart.setOption(option);
                }
                   window.addEventListener('resize', myChart.resize); </script>
                    })();
                    
<script type="text/javascript">
//<![CDATA[

          (function(){
            var dom = document.getElementById('resultGraph');
            var myChart = echarts.init(dom, 'dark');
            var option = {
                title: { text: 'Session Performance',subtext: 'Course Result', left: 'center' },
                tooltip: {
                    trigger: 'item',
                    formatter: function(p) {
                        return '<b>' + p.name + '</b><br/>' + p.marker + p.data.course + ' (' + p.data.value + '%, ' + p.data.grade + ')';
                    }
                },
                xAxis: { type: 'category', data: ['SP25','FA25'] },
                yAxis: { type: 'value', max: 100 },
                series: [
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 83, course: 'Functional English', grade: 'A-' },{ value: 75, course: 'Applied Physics', grade: 'B+' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 74, course: 'Pre-Calculus I', grade: 'B' },{ value: 82, course: 'Pre-Calculus II', grade: 'A-' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 87, course: 'Islamic Studies', grade: 'A' },{ value: 80, course: 'Technical and Business Writing', grade: 'A-' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 90, course: 'Programming Fundamentals', grade: 'A' },{ value: 75, course: 'Ideology and Constitution of Pakistan', grade: 'B+' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 83, course: 'Applications of Information and Communication Technologies', grade: 'A-' },{ value: 89, course: 'Object Oriented Programming', grade: 'A' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 100, course: 'Civics and Community Engagement', grade: 'A' },{ value: 80, course: 'Discrete Structures', grade: 'A-' }]
            },
            {
                type: 'bar',
                barGap: '0%', 
                label: { show: true, position: 'top', formatter: '{c}%' },
                itemStyle: {
                    color: function(p) {
                       var colors = { 'Pass': '#006400', 'A+': '#006400', 'A': '#228B22', 'A-': '#32CD32', 
                        'B+': '#9ACD32', 'B': '#ADFF2F', 'B-': '#FFFF00', 
                        'C+': '#FFD700', 'C': '#FFA500', 'C-': '#FF8C00', 
                        'D+': '#FF7F50', 'D': '#FF4500', 'D-': '#FF6347', 
                        'F': '#D22B2B', 'IF': '#880808', 'IP': '#7E1E1E', 'W': '#4A0404' 
                        }; 
                            return p.data ? (colors[p.data.grade] || '#5470c6') : '#5470c6';
                    }
                },
                data: [{ value: 95, course: 'Fundamentals of Psychology', grade: 'A' },null]
            }]
            };
            myChart.setOption(option);
            window.addEventListener('resize', myChart.resize);
        })();//]]>
</script>
</form>
</body>
</html>
