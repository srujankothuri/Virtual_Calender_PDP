
Working with other code (Part 4)
================

# 1 Context

This assignment gives you the experience of working with code that you did not write or design. The primary learning objective of this assignment is to understand code and design not written by you to the extent of implementing a new end-to-end feature using it. There are two secondary learning objectives. In sharing your code with another group you will attempt to communicate your ideas effectively to them, and answer questions posed by them as needed (i.e. do customer service). In addition you will be asking technically appropriate and precise questions to the group whose code you are working with. You will summarize your experience by critiquing the code given to you.

This assignment will be due in two parts, to ensure that each group is sending their code to another in a timely fashion, and receiving code in time to complete this assignment.

# 2 Prepare to send and receive code

You will receive the contact details of two different groups from us: 
one group that will give you their code (your provider) and another 
to which you must send your code (your customer).

In order to prepare your code to your customer, please do the following:

1. Complete the questionnaire in  "questionnaire.txt" included with the repository for this assignment in Pawtograder. This will summarize for your customer the status of your code (i.e. what works and what does not).
2. Prepare a zip file with ONLY the following. DO NOT include any hidden files or folders such as .gitignore, .github, .git etc.
	- the src/ directory
	- the res/ directory
	- the USEME markdown
	- the README markdown
	- build.gradle
	- questionnaire.txt

As soon as you receive the details of your provider group, please contact them and request that they send you their code as soon as possible. Please be patient as sometimes it takes a day or two for them to respond.

As soon as you receive the details of your customer group, please send them the above zip promptly and request that they acknowledge they received it. Further, submit the code you sent along with the questionnaire to the Pawtograder assignment repo for Assignment 5 Part 1 by the due date. This submission
documents and acknowledges that you have sent the code you were expected to send to the customer. Failing to submit part 1 will imply that you did not send the code you were expected to send.

# 3 Code Critique

As customers of the providers' code, you have the opportunity to praise, critique, comment upon, and suggest improvements to their code. Write a short (8-10 paragraphs) review of their code. Your review should have the following sections: 

- design critique. In this part you are expected to clearly explain BOTH the benefits and limitations of the design. To complement your explanation you must identify at least 3 code smells and  explain why they are applicable along with a brief explanation of how they can be refactored to remove the code smell. You can also identify a code smell and explain why it is reasonable to ignore the smell given the current design. For full credit the code smells you identify and explain cannot be trivial ones such as comments.
- implementation critique. In this part you are expected to explain BOTH strengths and limitations of the implementation including choice of model representation and data structures used. You can argue from the perspective of non functional requirements such as performance, readability, maintainability, and extensibility.
- documentation critique and constructive suggestions on how to address various limitations. 

**Writing a disorganized review that is difficult to read will result in a point deduction, irrespective of its content**. 

**Writing a review that is AI generated will result in an automatic 0 and further action based on the course's plagiarism policy. If your submission is flagged we will quiz you to explain your review and your review process**.


In short, you should provide a code review that is well-reasoned and well-argued using the SOLID principles learned in this course.

# 4 New Feature: calendar analytics

The users of our calendar app have requested for a new feature that
will help them analyze and monitor their usage of the calendar app.
To this end, they have requested to show a dashboard or summary view of
the selected calendar in a particular date period with the following metrics:

- Total number of events
- Total number of events by subject
- Total number of events by weekdays
- Total number of events by week
- Total number of events by month
- Average number of events per day
- The busiest day and the least busy day
- Percentage of events that were online and not online. An event is online if its location is online.

**Note all metrics (listed above) should be shown for a datetime interval selected by the user.**

The analytics dashboard must be available in **both the GUI and the
text interface**. For the GUI, you are free to select an appropriate
layout as long as all the metrics are displayed clearly for the
selected interval. For the text interface, you should
support the following additional command:

`show calendar dashboard from <dateString> to <dateString>`

The `dateString` must be in `YYYY-MM-DD` format. Both dates are inclusive.

On typing this command, the user should be able to see all metrics
listed above in the interval specified by the user. 

This command must always be typed after the `use calendar` command.


## 4.1 What to do

1. Implement and test only the new features listed in the previous section.

2. You should perform test coverage analysis to improve the tests you wrote. However, this is not a requirement for this assignment.

3. Add support for this new feature through the text interface using the above command.

4. Add support for this new feature through the GUI appropriately.

4. Write a critique of the provided code.

# 5 Code Issues

In general, if the code provided to you does not successfully implement a required feature you are not responsible for fixing it. Your providers should fix it and send you an update: it is OK to ask them to do so. Conversely you are expected to fix your code if a required feature does not work.

However, you should not expect your providers to re-design their working code to make it "better" according to you.

If for some reason you do not get repaired code or explanations from your providers and this affects your ability to complete the required feature in this assignment, be sure to mention this in the USEME.

Specifically, here are the guidelines for how to proceed with the given code. If in the code provided to you:

1. text interface and the GUI work, implement the new features and expose through text interface and the GUI (most straightforward).

2. the text interface works but GUI does not: implement new features and expose through text interface. Ask providers to fix the GUI enough for you to display the dashboard, and then add the dashboard feature to the GUI. If you were not able to completely expose through the GUI, mention in USEME.

3. If the code works, text interface and GUI somewhat works or does not work at all, implement the new feature and try to add the new text command. Ask the provider to fix the GUI, so you can add the dashboard. If unable to complete, mention in USEME.

4. If nothing works (no GUI, no script, even model is unworkable), such that you cannot even implement the new features: contact your professor, and we will assign you another provider.

Use your judgment to determine if you should wait for your providers to fix something, or for you to temporarily fix it to make progress. **You should not simply wait for your providers to fix problems shown by you, and then claim you could not complete the assignment because you were waiting for them.**

# 6 Grading Standards

For this assignment, you will be graded on the following:

1. The coherence and thoroughness of your review. Remember your review must be constructive so that a reader can read it, understand the strengths and limitations of the design, and take actionable steps to improve their design if required.

2. The correctness of the new feature implemented.

3. The completeness and correctness of the corresponding tests.

In general, you will not be penalized for bugs in the code sent to you (with the exception of style which you may have to fix). If this has hampered your ability to complete the assignment, please describe so explicitly in your submission. If your submission is found to not work, with no explanation as to why, we will assume that you are responsible for its problems. All related explanation should go in the USEME.

You will not be graded (much) on the code you send to your customers. It will simply be style-checked, and worth a small portion of your overall grade for the assignment, to ensure that you submit something we can compare against your customers' use of your code.

Your grade will generally not be affected by your customers' review of your code. Your grade may be affected by your responsiveness (or lack thereof) and experience with your customers.

# 7 Submission Requirements for Part 1 (This Assignment)

Submit the code you sent to your customer. For full credit you only
need to pass style and make sure you submitted the questionnaire.
Successful submission here will serve as acknowledgement that you sent the code you were expected to send to your customer.

# 8 Submission Requirements for Part 2

The instructions below apply to part 2 of the assignment, that is, a separate Pawtograder repository.

- Submit all files necessary to make your code work (this includes the code you got from your providers, with code you wrote for this assignment).
- A USEME markdown 
  - with instructions to run your program in different modes using examples.
  - instructions on how to test the newly added feature to the GUI. Add screenshots if it helps.
  - Explanation of bugs (if any) in sent code that hampered your ability to complete the requested features.
- Submit a `res/` folder with the following:
  - A screenshot showing the new feature in the GUI
  - A txt file, `commands.txt`, with the list of valid commands including the newly added commands.
  - A txt file, `invalid.txt` with a list of commands where at least one invalid command is a newly added command.
  - A markdown file `CHANGES` that specifically describes how you implemented the dashboard features to be in harmony with the design given to you (i.e. how you managed to fit the new feature in the existing design). Also indicate at the top of this file if you were able to implement the dashboard correctly, supported a text command for it and exposed it through the GUI (in the same style as the questionnaire you sent to your customers).
  - A markdown file `REVIEW` that documents your code critique.