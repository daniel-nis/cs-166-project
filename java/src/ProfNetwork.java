/*
 * Template JAVA User Interface
 * =============================
 *
 * Database Management Systems
 * Department of Computer Science &amp; Engineering
 * University of California - Riverside
 *
 * Target DBMS: 'Postgres'
 *
 */

import java.io.*;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * This class defines a simple embedded SQL utility class that is designed to
 * work with PostgreSQL JDBC drivers.
 *
 */

public class ProfNetwork {

    // reference to physical database connection.
    private Connection _connection = null;

    // handling the keyboard inputs through a BufferedReader
    // This variable can be global for convenience.
    static BufferedReader in = new BufferedReader(
            new InputStreamReader(System.in));

    /**
     * Creates a new instance of Messenger
     *
     * @param hostname the MySQL or PostgreSQL server hostname
     * @param database the name of the database
     * @param username the user name used to login to the database
     * @param password the user login password
     * @throws java.sql.SQLException when failed to make a connection.
     */
    public ProfNetwork (String dbname, String dbport, String user, String passwd) throws SQLException {

        System.out.print("Connecting to database...");
        try{
            // constructs the connection URL
            String url = "jdbc:postgresql://localhost:" + dbport + "/" + dbname;
            System.out.println ("Connection URL: " + url + "\n");

            // obtain a physical connection
            this._connection = DriverManager.getConnection(url, user, passwd);
            System.out.println("Done");
        }catch (Exception e){
            System.err.println("Error - Unable to Connect to Database: " + e.getMessage() );
            System.out.println("Make sure you started postgres on this machine");
            System.exit(-1);
        }//end catch
    }//end ProfNetwork

    /**
     * Method to execute an update SQL statement.  Update SQL instructions
     * includes CREATE, INSERT, UPDATE, DELETE, and DROP.
     *
     * @param sql the input SQL string
     * @throws java.sql.SQLException when update failed
     */
    public void executeUpdate (String sql) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement ();

        // issues the update instruction
        stmt.executeUpdate (sql);

        // close the instruction
        stmt.close ();
    }//end executeUpdate

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and outputs the results to
     * standard out.
     *
     * @param query the input query string
     * @return the number of rows returned
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int executeQueryAndPrintResult (String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement ();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery (query);

        /*
         ** obtains the metadata object for the returned result set.  The metadata
         ** contains row and column info.
         */
        ResultSetMetaData rsmd = rs.getMetaData ();
        int numCol = rsmd.getColumnCount ();
        int rowCount = 0;

        // iterates through the result set and output them to standard out.
        boolean outputHeader = true;
        while (rs.next()){
            if(outputHeader){
                for(int i = 1; i <= numCol; i++){
                    System.out.print(rsmd.getColumnName(i) + "\t");
                }
                System.out.println();
                outputHeader = false;
            }
            for (int i=1; i<=numCol; ++i)
                System.out.print (rs.getString (i) + "\t");
            System.out.println ();
            ++rowCount;
        }//end while
        stmt.close ();
        return rowCount;
    }//end executeQuery

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and returns the results as
     * a list of records. Each record in turn is a list of attribute values
     *
     * @param query the input query string
     * @return the query result as a list of records
     * @throws java.sql.SQLException when failed to execute the query
     */
    public List<List<String>> executeQueryAndReturnResult (String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement ();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery (query);

        /*
         ** obtains the metadata object for the returned result set.  The metadata
         ** contains row and column info.
         */
        ResultSetMetaData rsmd = rs.getMetaData ();
        int numCol = rsmd.getColumnCount ();
        int rowCount = 0;

        // iterates through the result set and saves the data returned by the query.
        boolean outputHeader = false;
        List<List<String>> result  = new ArrayList<List<String>>();
        while (rs.next()){
            List<String> record = new ArrayList<String>();
            for (int i=1; i<=numCol; ++i)
                record.add(rs.getString (i));
            result.add(record);
        }//end while
        stmt.close ();
        return result;
    }//end executeQueryAndReturnResult

    /**
     * Method to execute an input query SQL instruction (i.e. SELECT).  This
     * method issues the query to the DBMS and returns the number of results
     *
     * @param query the input query string
     * @return the number of rows returned
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int executeQuery (String query) throws SQLException {
        // creates a statement object
        Statement stmt = this._connection.createStatement ();

        // issues the query instruction
        ResultSet rs = stmt.executeQuery (query);

        int rowCount = 0;

        // iterates through the result set and count nuber of results.
        if(rs.next()){
            rowCount++;
        }//end while
        stmt.close ();
        return rowCount;
    }

    /**
     * Method to fetch the last value from sequence. This
     * method issues the query to the DBMS and returns the current
     * value of sequence used for autogenerated keys
     *
     * @param sequence name of the DB sequence
     * @return current value of a sequence
     * @throws java.sql.SQLException when failed to execute the query
     */
    public int getCurrSeqVal(String sequence) throws SQLException {
        Statement stmt = this._connection.createStatement ();

        ResultSet rs = stmt.executeQuery (String.format("Select currval('%s')", sequence));
        if (rs.next())
            return rs.getInt(1);
        return -1;
    }

    /**
     * Method to close the physical connection if it is open.
     */
    public void cleanup(){
        try{
            if (this._connection != null){
                this._connection.close ();
            }//end if
        }catch (SQLException e){
            // ignored.
        }//end try
    }//end cleanup

    /**
     * The main execution method
     *
     * @param args the command line arguments this inclues the <mysql|pgsql> <login file>
     */
    //Making authorised user a global variable
    String authorisedUser;
    public static void main (String[] args) {
        if (args.length != 3) {
            System.err.println (
                    "Usage: " +
                            "java [-classpath <classpath>] " +
                            ProfNetwork.class.getName () +
                            " <dbname> <port> <user>");
            return;
        }//end if

        Greeting();
        ProfNetwork esql = null;
        try{
            // use postgres JDBC driver.
            Class.forName ("org.postgresql.Driver").newInstance ();
            // instantiate the Messenger object and creates a physical
            // connection.
            String dbname = args[0];
            String dbport = args[1];
            String user = args[2];
            esql = new ProfNetwork (dbname, dbport, user, "dees");

            boolean keepon = true;
            while(keepon) {
                // These are sample SQL statements
                System.out.println("MAIN MENU");
                System.out.println("---------");
                System.out.println("1. Create user");
                System.out.println("2. Log in");
                System.out.println("9. < EXIT");
                String authorisedUser = null;
                switch (readChoice()){
                    case 1: CreateUser(esql); break;
                    case 2: esql.authorisedUser = LogIn(esql); break; // Change made
                    case 9: keepon = false; break;
                    default : System.out.println("Unrecognized choice!"); break;
                }//end switch
                if (esql.authorisedUser != null) {
                    boolean usermenu = true;
                    while(usermenu) {
                        System.out.println("MAIN MENU");
                        System.out.println("---------");
                        System.out.println("1. Goto Friend List");
                        System.out.println("2. Update Profile");
                        System.out.println("3. Write a new message");
                        System.out.println("4. Send Friend Request");
                        System.out.println("5. Search Users by Name");
                        System.out.println("6. View Message Inbox");
                        System.out.println("7. View Incoming Requests");
                        System.out.println("8. View a Profile");
                        System.out.println(".........................");
                        System.out.println("9. Log out");
                        switch (readChoice()){
                            case 1: FriendList(esql); break;
                            case 2: updateProfile(esql); break;
                            case 3: NewMessage(esql); break;
                            case 4: SendRequest(esql); break;
                            case 5: Search(esql); break;
                            case 6: ViewMessages(esql, esql.authorisedUser); break;
                            case 7: IncomingRequests(esql, esql.authorisedUser); break;
                            case 8: ViewProfile(esql, esql.authorisedUser); break;
                            case 9: usermenu = false; break;
                            default : System.out.println("Unrecognized choice!"); break;
                        }
                    }
                }
            }//end while
        }catch(Exception e) {
            System.err.println (e.getMessage ());
        }finally{
            // make sure to cleanup the created table and close the connection.
            try{
                if(esql != null) {
                    System.out.print("Disconnecting from database...");
                    esql.cleanup ();
                    System.out.println("Done\n\nBye !");
                }//end if
            }catch (Exception e) {
                // ignored.
            }//end try
        }//end try
    }//end main

    public static void Greeting(){
        System.out.println(
                "\n\n*******************************************************\n" +
                        "              User Interface      	               \n" +
                        "*******************************************************\n");
    }//end Greeting

    /*
     * Reads the users choice given from the keyboard
     * @int
     **/
    public static int readChoice() {
        int input;
        // returns only if a correct value is given.
        do {
            System.out.print("Please make your choice: ");
            try { // read the integer, parse it and break.
                input = Integer.parseInt(in.readLine());
                break;
            }catch (Exception e) {
                System.out.println("Your input is invalid!");
                continue;
            }//end try
        }while (true);
        return input;
    }//end readChoice

    /*
     * Creates a new user with privided login, passowrd and phoneNum
     * An empty block and contact list would be generated and associated with a user
     **/
    public static void CreateUser(ProfNetwork esql){
        try{
            System.out.print("\tEnter user login: ");
            String login = in.readLine();
            System.out.print("\tEnter user password: ");
            String password = in.readLine();
            System.out.print("\tEnter user email: ");
            String email = in.readLine();

            //Creating empty contact\block lists for a user
            String query = String.format("INSERT INTO USR (userId, password, email) VALUES ('%s','%s','%s')", login, password, email);

            esql.executeUpdate(query);
            System.out.println ("User successfully created!");
        }catch(Exception e){
            System.err.println (e.getMessage ());
        }
    }//end

    /*
     * Check log in credentials for an existing user
     * @return User login or null is the user does not exist
     **/
    public static String LogIn(ProfNetwork esql){
        try{
            System.out.print("\tEnter user login: ");
            String login = in.readLine();
            System.out.print("\tEnter user password: ");
            String password = in.readLine();

            String query = String.format("SELECT * FROM USR WHERE userId = '%s' AND password = '%s'", login, password);
            int userNum = esql.executeQuery(query);
            if (userNum > 0)
                return login;
            return null;
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return null;
        }
    }//end

// Rest of the functions definition go in here

    public static class UpdateProfile {
        private ProfNetwork esql;

        UpdateProfile(ProfNetwork esql) {
            this.esql = esql;
        }

        public void password() throws Exception{
            Console console = System.console();
            System.out.print("\tEnter new password: ");
            String password = in.readLine();
            System.out.print("\tEnter confirm password: ");
            String cpassword = in.readLine();
            if(password.compareTo(cpassword) == 0){
                String query = String.format("UPDATE usr set password = '%s' WHERE userId = '%s'", password, esql.authorisedUser);
                esql.executeUpdate(query);
                System.out.println("****** PASSWORD UPDATED ******");
            }else{
                System.out.println("Password doesn't match");
            }
            halt();
        }
        public void email() throws Exception {
            System.out.print("\tEnter Email: ");
            String email = in.readLine();
            String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
                    "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
            if(Pattern.compile(emailRegex).matcher(email).matches()) {
                String query = String.format("UPDATE usr set email = '%s' WHERE userId = '%s'", email, esql.authorisedUser);
                System.out.println(query);
                esql.executeUpdate(query);
                System.out.println("****** EMAIL UPDATED ******");
            } else {
                System.out.println("NOT A VALID INPUT");
            }
            halt();
        }
        public void fullName() throws Exception {
            System.out.print("\tEnter Full Name: ");
            String fullName = in.readLine();
            String query = String.format("UPDATE usr set name = '%s' WHERE userId = '%s'", fullName, esql.authorisedUser);
            esql.executeUpdate(query);
            System.out.println("****** FULL NAME UPDATED ******");
            halt();
        }
        public void dateOfBirth() throws Exception {
            System.out.print("\tEnter Date of Birth: ");
            String dateOfBirth = in.readLine();
            String query = String.format("UPDATE usr set dateOfBirth = '%s' WHERE userId = '%s'", dateOfBirth, esql.authorisedUser);
            System.out.println(query);
            esql.executeUpdate(query);
            System.out.println("****** DATE of BIRTH UPDATED ******");
            halt();
        }
    }
    public static void FriendList(ProfNetwork esql) throws Exception{
        String query = String.format("select connectionId from connection_usr where userId = '%s' and status ='Accept'", esql.authorisedUser);
        int numOfFriends = esql.executeQueryAndPrintResult(query);
        System.out.println("Total number of friends: " + numOfFriends);
        halt();
    }
    public static void updateProfile(ProfNetwork esql) throws Exception{
        UpdateProfile update = new UpdateProfile(esql);
        do{
            System.out.println("UPDATE MENU");
            System.out.println("------------");
            System.out.println("1. Update Password");
            System.out.println("2. Update Email");
            System.out.println("3. Update Full name");
            System.out.println("4. Update Date of Birth");
            System.out.println("..........................");
            System.out.println("9. Back to main menu");
            switch(readChoice()){
                case 1: update.password();return;
                case 2: update.email(); return;
                case 3: update.fullName();return;
                case 4: update.dateOfBirth();return;
                case 9: return;
                default: System.out.println("Invalid Choice");halt();return;
            }
        }while(true);
    }
    public static void NewMessage(ProfNetwork esql) throws Exception{

        System.out.print("\tEnter your friend ID : ");
        String friendId = in.readLine();
        String queryToCheckFriend = String.format("Select count(*) from connection_usr where userid = '%s' AND userId IN " +
                "(Select connectionId from connection_usr where userId = '%s' AND status = 'Accept')", friendId, esql.authorisedUser);
        int check = esql.executeQuery(queryToCheckFriend);
        if(check > 0){
            // Can Send message
            String queryToGetMsgId = String.format("Select MAX(msgId) from message");
            List<List<String>> res = esql.executeQueryAndReturnResult(queryToGetMsgId);
            int msgId = Integer.parseInt(res.get(0).get(0)) + 1;
            System.out.print("\tEnter message : ");
            String message = in.readLine();
            String timestamp = (java.sql.Timestamp.from(Instant.now())).toString();
            String query = String.format("INSERT into message (msgId,senderId,receiverId,contents,sendTime,deleteStatus,status) " +
                    "values('%s','%s','%s','%s','%s','0','Sent')",
                    msgId, esql.authorisedUser, friendId, message, timestamp);
            esql.executeUpdate(query);
            System.out.println("****** MESSAGE SENT ******");
        }else{
            System.out.println("Not your connection cannot send messages");
        }
        halt();
    }
    public static void SendRequest(ProfNetwork esql) throws Exception{
        System.out.print("\tEnter the user id to send request : ");
        String newConnection = in.readLine();
        String queryToCheckIfUserExists = String.format("Select * from usr where userid = '%s'", newConnection);
        int check1 = esql.executeQuery(queryToCheckIfUserExists);
        if(check1 == 0){
            System.out.println("Connecion ID does not exist");
            halt();
            return;
        }
        String queryToCheckIfAlreadySent = String.format("Select * from connection_usr where userid = '%s' AND connectionid = '%s'", esql.authorisedUser, newConnection);
        List<List<String>> res = esql.executeQueryAndReturnResult(queryToCheckIfAlreadySent);
//         System.out.println(res.size());
        if(res.size() > 0){
            System.out.println("Friend Request is already sent");
            System.out.format("Status of friend request to %s is %s \n", newConnection, res.get(res.size()-1).get(2));
            halt();
            return;
        }
        String queryToCheckNoOfFriends = String.format("Select * from connection_usr where userid = '%s'", esql.authorisedUser);
        int check2 = esql.executeQuery(queryToCheckNoOfFriends);
        if(check2 < 5){
            // Send request
            String query = String.format("insert into connection_usr values('%s','%s','Request')", esql.authorisedUser, newConnection);
            esql.executeUpdate(query);
            System.out.println("****** FRIEND REQUEST SENT ******");
            halt();
            return;
        }
        String queryToCheckIfCanSend = String.format("select count(*) from connection_usr where userid = '%s' and userid IN " +
                "(select connectionid from connection_usr where userid IN " +
                "(select connectionid from connection_usr where userid='%s'))", newConnection, esql.authorisedUser);
        int check3 = esql.executeQuery(queryToCheckIfCanSend);
        if(check3 > 0){
            //Send Request
            String query = String.format("insert into connection_usr values('%s','%s','Request')", esql.authorisedUser, newConnection);
            esql.executeUpdate(query);
            System.out.println("****** FRIEND REQUEST SENT ******");
        }else{
            System.out.println("Cannot Send Request");
        }
        halt();
    }
    public static void halt() throws Exception{
        System.out.println("Press any key to continue");
        in.readLine();
    }

    public static void Search(ProfNetwork esql){
        try{
            System.out.print("\tEnter a name to search: ");
            String name = in.readLine();

            String query = String.format("SELECT U.userid, U.name FROM USR U WHERE U.name = '%s'", name);
            //String query = String.format("SELECT U.userid, U.name FROM USR U WHERE U.userid EXISTS IN (SELECT C.userid FROM Connection C WHERE C.userid EXISTS IN (SELECT U1.userid FROM User U1 WHERE U1.name LIKE %" + name + "%))");
            int userNum = esql.executeQueryAndPrintResult(query);
            if (userNum <= 0){
                System.out.printf("\nNo results for: %s\n", name);
                return;
            }
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return;
        }
    }


    public static void ViewProfile(ProfNetwork esql, String auth){
        try{
            boolean select = true;
            while(select){
                System.out.println("1. View Your Profile ");
                System.out.println("2. View a Friend's Profile");
                System.out.println("9. Cancel");

                switch(readChoice()){
                    case 1:
                        String query = String.format("SELECT U.userid, U.name, U.email, U.dateofbirth FROM USR U WHERE U.userid = '%s'", auth);
                        esql.executeQueryAndPrintResult(query);
                        String query1 = String.format("SELECT W.company, W.role, W.location, W.startdate, W.enddate FROM WORK_EXPR W WHERE W.userid = '%s'", auth);
                        esql.executeQueryAndPrintResult(query1);
                        String query2 = String.format("SELECT E.instituitionname, E.major, E.degree, E.startdate, E.enddate FROM EDUCATIONAL_DETAILS E WHERE E.userid = '%s'", auth);
                        esql.executeQueryAndPrintResult(query2);
                        select = false;
                        break;
                    case 2:
                        System.out.print("\tEnter the username for the profile you want to view: ");
                        String name = in.readLine();

         	   /*String query3 = String.format("SELECT U.userid, U.name FROM USR U WHERE U.userid EXISTS IN (SELECT C.userid FROM Connection C WHERE C.userid EXISTS IN (SELECT U1.userid FROM User U1 WHERE U1.name LIKE %" + name + "%))");
         	   int userNum = esql.executeQueryAndPrintResult(query3);
         	   if (userNum <= 0){
            	      System.out.printf("\nNo results for: %s\n", name);
            	   return;
         	   }*/
                        String query3 = String.format("SELECT U.userid, U.name, U.email, U.dateofbirth FROM USR U WHERE U.userid = '%s'", name);
                        esql.executeQueryAndPrintResult(query3);
                        String query4 = String.format("SELECT W.company, W.role, W.location, W.startdate, W.enddate FROM WORK_EXPR W WHERE W.userid = '%s'", name);
                        esql.executeQueryAndPrintResult(query4);
                        String query5 = String.format("SELECT E.instituitionname, E.major, E.degree, E.startdate, E.enddate FROM EDUCATIONAL_DETAILS E WHERE E.userid = '%s'", name);
                        esql.executeQueryAndPrintResult(query5);
                        select = false;
                        break;
                    case 9:
                        select = false;
                        break;
                }
            }
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return;
        }
    }

    public static void ViewMessages(ProfNetwork esql, String auth){
        try{
            boolean select = true;
            while(select){
                System.out.println("1. View Messages in Inbox ");
                System.out.println("2. View Sent Messages");
                System.out.println("3. Write a New Message");
                System.out.println("9. Cancel");

                switch(readChoice()){
                    case 1:
                        System.out.print("\n\tMessages in inbox: \n");
                        String query = String.format("SELECT M.msgid, M.senderid, M.contents FROM Message M WHERE M.receiverid = '%s' AND (M.deleteStatus = 0 OR M.deleteStatus = 2)", auth);
                        esql.executeQueryAndPrintResult(query);
                        DeleteMessage(esql, auth, "rec");
                        select = false;
                        break;
                    case 2:
                        System.out.print("\n\tMessages you've sent: \n");
                        String query2 = String.format("SELECT M.msgid, M.receiverid, M.contents FROM Message M WHERE M.senderid = '%s' AND (M.deleteStatus = 0 OR M.deleteStatus = 1)", auth);
                        esql.executeQueryAndPrintResult(query2);
                        DeleteMessage(esql, auth, "sender");
                        select = false;
                        break;
                    case 3:
                        System.out.println("See");
                        break;
                    case 9:
                        System.out.println("Cancel");
                        select = false;
                        break;
                }
            }
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return;
        }
    }

    public static void DeleteMessage(ProfNetwork esql, String auth, String who){
        try{
            boolean select = true;
            while(select){
                System.out.println("Would you like to delete a message?");
                System.out.println("1. Delete a message");
                System.out.println("2. Return to Main Menu");
                switch(readChoice()){
                    case 1:
                        System.out.print("\tEnter the Message ID which you want to delete: \n");
                        String delMsg = in.readLine();
                        String query = String.format("SELECT M.deleteStatus FROM MESSAGE M WHERE M.msgid = '%s'", delMsg);
                        List<List<String>> status = new ArrayList<List<String>>(esql.executeQueryAndReturnResult(query));
                        if(who == "sender") {
                            if(Integer.parseInt(status.get(0).get(0)) == 0){         // 0 = nobody deleted
                                String query0 = String.format("UPDATE Message SET deleteStatus = 2 WHERE msgid = '%s'", delMsg);
                                esql.executeUpdate(query0);
                            }
                            else if(Integer.parseInt(status.get(0).get(0)) == 1){	// 1 = receiver deleted
                                String query1 = String.format("UPDATE Message SET deleteStatus = 3 WHERE msgid = '%s'", delMsg);
                                esql.executeUpdate(query1);
                            }							// 2 = sender deleted
                            else {							// 3 = all deleted
                                System.out.println("This message has already been deleted");
                            }
                        }
                        else {
                            if(Integer.parseInt(status.get(0).get(0)) == 0){	// 0 = nobody deleted
                                String query0 = String.format("UPDATE Message SET deleteStatus = 1 WHERE msgid = '%s'", delMsg);
                                esql.executeUpdate(query0);
                            }
                            else if(Integer.parseInt(status.get(0).get(0)) == 2){	// 2 = sender deleted
                                String query2 = String.format("UPDATE Message SET deleteStatus = 3 WHERE msgid = '%s'", delMsg);
                                esql.executeUpdate(query2);
                            }							// 1 = reciever deleted
                            else {							// 3 = all deleted
                                System.out.println("This message has already been deleted");
                            }
                        }
                        break;
                    case 2:
                        select = false;
                        break;
                }
            }
            //System.out.print("\tEnter the Message ID which you want to delete: \n");
            //String delMsg = in.readLine();

            //String query = String.format("SELECT M.msgid, M.senderid, M.contents FROM Message M WHERE M.receiverid = '%s'", auth);
            //int userNum = esql.executeQueryAndPrintResult(query);
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return;
        }
    }

    public static void IncomingRequests(ProfNetwork esql, String auth){
        try{
            System.out.print("\tIncoming Requests: ");

            String query = String.format("SELECT C.connectionid FROM Connection_USR C WHERE C.userid = '%s' AND C.status = 'Request'", auth);
            int userNum = esql.executeQueryAndPrintResult(query);

            System.out.println("\tEnter a connection id to accept/reject their request status: ");
            String connect = in.readLine();
            // todo: add lookup to check whether input is valid
            String stat = "Request";
            boolean select = true;
            while(select){
                System.out.printf("\n\tFor %s, would you like to accept or reject their request?", connect);
                System.out.println("\n\t1. Accept ");
                System.out.println("\t2. Reject ");
                System.out.println("\n\t9. Cancel ");

                switch(readChoice()){
                    case 1:
                        String accQuery = String.format("UPDATE Connection_usr SET status = 'Accept' WHERE userid = '%s' AND connectionid = '%s'", auth, connect);
                        esql.executeQuery(accQuery);
                        select = false;
                        break;
                    case 2:
                        String rejQuery = String.format("UPDATE Connection_usr SET status = 'Reject' WHERE userid = '%s' AND connectionid = '%s'", auth, connect);
                        esql.executeQuery(rejQuery);
                        select = false;
                        break;
                    case 9:
                        select = false;
                        break;
                    default:
                        System.out.println("\tYour choice is not recognized");
                        break;
                }
            }
        }catch(Exception e){
            System.err.println (e.getMessage ());
            return;
        }
    }



}//end ProfNetwor
