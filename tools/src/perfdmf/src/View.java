package edu.uoregon.tau.perfdmf;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import edu.uoregon.tau.perfdmf.database.DB;
import edu.uoregon.tau.perfdmf.database.DBConnector;

/**
 * This class is the RMI class which contains the tree of views to be 
 * constructed in the PerfExplorerClient.
 *
 * <P>CVS $Id: RMIView.java,v 1.11 2009/02/27 00:45:09 khuck Exp $</P>
 * @author khuck
 * @version 0.1
 * @since   0.1
 *
 */
public class View implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7198343642137106238L;
	private static List<String> fieldNames = null;
	protected List<String> fields = null;
	private DefaultMutableTreeNode node = null;
	private View parent = null;
    private Database database;
    private int viewID = 0;
    private String whereClause = "";
    private String joinClause = "";
    private String trialID = "";

    /**
	 * @return the trialID
	 */
	public String getTrialID() {
		return trialID;
	}

	/**
	 * @param trialID the trialID to set
	 */
	public void setTrialID(String trialID) {
		this.trialID = trialID;
	}

	/**
	 * @return the joinClause
	 */
	public String getJoinClause() {
		if (trialID.length() > 0) {
			return "";
		}
		return joinClause;
	}

	/**
	 * @param joinClause the joinClause to set
	 */
	public void setJoinClause(String joinClause) {
		this.joinClause = joinClause;
	}

	public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

	public View getParent() {
		return parent;
	}

	public void setParent(View parent) {
		this.parent = parent;
	}

	public View () {
		fields = new ArrayList<String>();
	}

	public View(View view) {
		this.fields = new ArrayList<String>();
		for (String f : view.fields) {
			this.fields.add(f);
		}
		this.node = view.node;
		this.parent = view.parent;
		this.database = view.database;
		this.viewID = view.viewID;
	}


	public static Iterator<String> getFieldNames(DB db) {
		String allUpperCase = "TRIAL_VIEW";
		String allLowerCase = "trial_view";
		if (db.getSchemaVersion() > 0) {
			allUpperCase = "TAUDB_VIEW";
			allLowerCase = "taudb_view";
		}
		if (fieldNames == null) {
			fieldNames = new ArrayList<String>();
			try {
				ResultSet resultSet = null;
				DatabaseMetaData dbMeta = db.getMetaData();
				if (db.getDBType().compareTo("oracle") == 0) {
					resultSet = dbMeta.getColumns(null, null, allUpperCase, "%");
				} else if (db.getDBType().compareTo("derby") == 0) {
					resultSet = dbMeta.getColumns(null, null, allUpperCase, "%");
				} else if (db.getDBType().compareTo("h2") == 0) {
					resultSet = dbMeta.getColumns(null, null, allUpperCase, "%");
				} else if (db.getDBType().compareTo("db2") == 0) {
					resultSet = dbMeta.getColumns(null, null, allUpperCase, "%");
				} else {
					resultSet = dbMeta.getColumns(null, null, allLowerCase, "%");
				}

				while (resultSet.next() != false) {
					String name =
					resultSet.getString("COLUMN_NAME").toUpperCase();
					fieldNames.add(name);
				}
				resultSet.close();
			} catch (SQLException e) {
				System.err.println("DATABASE EXCEPTION: " + e.toString());
				e.printStackTrace();
			}
		}
		return fieldNames.iterator();
	}

	public static Iterator<String> getFieldNames() {
		// assumes not null!
		return fieldNames.iterator();
	}

	public static int getFieldCount() {
		return fieldNames.size();
	}

	public void addField(String value) {
		fields.add(value);
	}

	public String getField(String fieldName) {
		int i = fieldNames.indexOf(fieldName.toUpperCase());
		if (i == -1)
			return new String("");
		else
			if (fields == null)
				return "";
			return fields.get(i);
	}

	public String getField(int i) {
		return fields.get(i);
	}

	public static String getFieldName(int i) {
		return fieldNames.get(i);
	}

	// suppress warning about aInputStream.readObject() call.
	@SuppressWarnings("unchecked")
	private void readObject (ObjectInputStream aInputStream) throws ClassNotFoundException, IOException {
		// perform the default serialization for this object
		aInputStream.defaultReadObject();
		if (fieldNames == null)
			fieldNames = (List<String>) aInputStream.readObject();
	}

	private void writeObject (ObjectOutputStream aOutputStream) throws IOException {
		// perform the default serialization for this object
		aOutputStream.defaultWriteObject();
		aOutputStream.writeObject(fieldNames);
	}

	public String toString() {
		return "View: " + getField("NAME");
	}

	public void setDMTN(DefaultMutableTreeNode node) {
		this.node = node;
	}

	public DefaultMutableTreeNode getDMTN() {
		return this.node;
	}
	
	/**
	 * Get the subviews for this parent.  If the parent is 0, get all top-level
	 * views.
	 * 
	 * @param parent
	 * @return List of views
	 */
	public static List<View> getViews (int parent, DB db) {
		//PerfExplorerOutput.println("getViews()...");
		List<View> views = new ArrayList<View>();
		try {
			String table_name = "trial_view";
			if (db.getSchemaVersion() > 0) {
				table_name = "taudb_view";
			}
			Iterator<String> names = View.getFieldNames(db);
			if (!names.hasNext()) {
				// the database is not modified to support views
				throw new Exception ("The Database is not modified to support views.");
			}
			StringBuilder buf = new StringBuilder("select ");
			// assumes at least one column...
			buf.append(names.next());
			while (names.hasNext()) {
				buf.append(", ");
				buf.append(names.next());
			}
			buf.append(" from " + table_name);
			if (parent == -1) { // get all views!
				// no while clause
			} else if (parent == 0) {
				buf.append(" where parent is null");
			} else {
				buf.append(" where parent = ");
				buf.append(parent);
			}
			PreparedStatement statement = db.prepareStatement(buf.toString());
			//PerfExplorerOutput.println(statement.toString());
			ResultSet results = statement.executeQuery();
			while (results.next() != false) {
				View view = new View();
				view.setDatabase(db.getDatabase());
				for (int i = 1 ; i <= View.getFieldCount() ; i++) {
					view.addField(results.getString(i));
				}
				views.add(view);
			}
			statement.close();
		} catch (Exception e) {
			String error = "ERROR: Couldn't select views from the database!";
			System.err.println(error);
			e.printStackTrace();
		}
		return views;
	}

	/**
	 * Get the trials which are filtered by the defined view.
	 * 
	 * @param views
	 * @return List
	 */
	public static List<Trial> getTrialsForView (List<View> views, boolean getXMLMetadata, DB db) {
		//PerfExplorerOutput.println("getTrialsForView()...");
		List<Trial> trials = null;
		if (db.getSchemaVersion() > 0) {
			trials = View.getTrialsForTAUdbView(views, db);
		} else {
			trials = new ArrayList<Trial>();
			try {
				StringBuilder whereClause = new StringBuilder();
				whereClause.append(" inner join application a on e.application = a.id ");
				whereClause.append(" where ");
				for (int i = 0 ; i < views.size() ; i++) {
					if (i > 0) {
						whereClause.append (" AND ");
					}
					View view = views.get(i);
					whereClause.append(view.getWhereClause(db.getDBType()));
				}
				//PerfExplorerOutput.println(whereClause.toString());
				trials = Trial.getTrialList(db, whereClause.toString(), getXMLMetadata);
			} catch (Exception e) {
				String error = "ERROR: Couldn't select views from the database!";
				System.err.println(error);
				e.printStackTrace();
			}
		}
		StringBuilder sb = new StringBuilder();
		boolean started = false;
		for (Trial trial : trials) {
			if (started) {
				sb.append(",");
			} else {
				sb.append("(");
			}
			sb.append(trial.trialID);
			started = true;
		}
		if (started) {
			sb.append(")");
		}
		for (View view : views) {
			// this is overkill for the sub-views, but this string will be updated if/when
			// they are expanded/selected
			view.setTrialID(sb.toString());
		}
		return trials;
	}

	/**
	 * Get the trials which are filtered by the defined view(s).
	 * 
	 * @param views
	 * @return List
	 */
	public static List<Trial> getTrialsForTAUdbView (List<View> views, DB db) {
		//PerfExplorerOutput.println("getTrialsForView()...");
		List<Trial> trials = new ArrayList<Trial>();
		HashMap<Integer, View> hashViews = new HashMap<Integer, View>();
		for(View view: views){
			hashViews.put(view.getID(), view);
		}
		return getTrialsForTAUdbView(views, hashViews, db);
	}


	private static List<Trial> getTrialsForTAUdbView(List<View> views,HashMap<Integer, View> hashViews, DB db) {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append("select conjoin, taudb_view, table_name, column_name, operator, value from taudb_view left outer join taudb_view_parameter on taudb_view.id = taudb_view_parameter.taudb_view where taudb_view.id in (");
			for (int i = 0 ; i < views.size(); i++) {
				if (i > 0)
					sql.append(",?");
				else
					sql.append("?");
			}
			sql.append(") order by taudb_view.id");
			PreparedStatement statement = db.prepareStatement(sql.toString());
			int i = 1;
			for (View view : views) {
				statement.setInt(i, Integer.valueOf(view.getField("ID")));
				i++;
			}
			ResultSet results = statement.executeQuery();
			
			StringBuilder whereClause = new StringBuilder();
			StringBuilder joinClause = new StringBuilder();
			int currentView = 0;
			int alias = 0;
			String conjoin = " where ";
			while (results.next() != false) {
				int viewid = results.getInt(2);
				String tableName = results.getString(3);
				if (tableName == null) 
					break;
				String columnName = results.getString(4);
				String operator = results.getString(5);
				String value = results.getString(6);
				if ((currentView > 0) && (currentView != viewid)) {
					conjoin = " and ";
				} else if (currentView == viewid) {
					conjoin = " " + results.getString(1) + " ";
				}
				if (tableName.equalsIgnoreCase("trial")) {
					whereClause.append(conjoin +   "t." + columnName + " " + operator + " " + "'" + value + "'");
				} else {
					// otherwise, we have primary_metadata or secondary_metadata
					joinClause.append(" left outer join " + tableName + " t" + alias + " on t.id = t" + alias + ".trial");
					whereClause.append(conjoin + "t" + alias + ".name = '" + columnName + "' ");
					whereClause.append("and  t" + alias + ".value "+operator+" '" + value + "' ");
				}
				alias++;
				currentView = viewid;
				hashViews.get(currentView).setWhereClause(whereClause.toString());
				hashViews.get(currentView).setJoinClause(joinClause.toString());
			}
			statement.close();
			
			//PerfExplorerOutput.println(whereClause.toString());

			return Trial.getTrialList(db, joinClause.toString() + " " + whereClause.toString(), false);
		} catch (Exception e) {
			String error = "ERROR: Couldn't select views from the database!";
			System.err.println(error);
			e.printStackTrace();
		}
		return null;
	}

	public int getID() {
		if (this.viewID == 0) {
			this.viewID = Integer.valueOf(this.getField("ID"));
		}
		return this.viewID;
	}
	
	public void setID(int id) {
		this.viewID = id;
	}
	public void setField(String name, String field){
		setField(fieldNames.indexOf(name.toUpperCase()), field);
	}
    public void setField(int idx, String field) {
        if (DBConnector.isIntegerType(database.getAppFieldTypes()[idx]) && field != null) {
            try {
                //int test = 
                	Integer.parseInt(field);
            } catch (java.lang.NumberFormatException e) {
                return;
            }
        }

        if (DBConnector.isFloatingPointType(database.getAppFieldTypes()[idx]) && field != null) {
            try {
                //double test = 
                	Double.parseDouble(field);
            } catch (java.lang.NumberFormatException e) {
                return;
            }
        }
        fields.set(idx, field);
    }
    public void rename(DB db, String newName) {
    	try{
        PreparedStatement statement = db.prepareStatement("UPDATE " + db.getSchemaPrefix() + "taudb_view SET name = ? WHERE id = ?");
        statement.setString(1, newName);
        statement.setInt(2, this.getID());

        statement.executeUpdate();
        setField("NAME",newName);
    	}catch (SQLException ex){
        	ex.printStackTrace();
        }
    }

	public int saveView(DB db) throws SQLException {
        boolean itExists = false;

        // First, determine whether it exists already (whether we are doing an insert or update)
        PreparedStatement statement = db.prepareStatement("SELECT name FROM " + db.getSchemaPrefix() + "taudb_view WHERE id = ?");
        statement.setInt(1, this.getID());
        ResultSet resultSet = statement.executeQuery();
        while (resultSet.next() != false) {
            itExists = true;
            break;
        }
        resultSet.close();
        statement.close();

        StringBuffer buf = new StringBuffer();
        if (itExists) {
            buf.append("UPDATE " + db.getSchemaPrefix() + "taudb_view SET ");
            for (int i = 0; i < this.getNumFields(); i++) {
            	if (!View.getFieldName(i).equals("ID"))
                    buf.append(", " + View.getFieldName(i) + " = ?");
            }
            buf.append(" WHERE id = ?");
        } else {
            buf.append("INSERT INTO " + db.getSchemaPrefix() + "taudb_view (name");
            for (int i = 0; i < this.getNumFields(); i++) {
            	if (!View.getFieldName(i).equals("ID"))
                    buf.append(", " + View.getFieldName(i));
            }
            buf.append(") VALUES (?");
            for (int i = 0; i < this.getNumFields(); i++) {
            	if (!View.getFieldName(i).equals("ID"))
                    buf.append(", ?");
            }
            buf.append(")");
        }

        statement = db.prepareStatement(buf.toString());

        int pos = 1;

        for (int i = 0; i < this.getNumFields(); i++) {
        	if (!View.getFieldName(i).equals("ID"))
                statement.setString(pos++, this.getField(i));
        }

        if (itExists) {
            statement.setInt(pos++, this.getID());
        }
        statement.executeUpdate();
        statement.close();

        int newViewID = 0;

        if (itExists) {
            newViewID = this.getID();
        } else {
            String tmpStr = new String();
            if (db.getDBType().compareTo("mysql") == 0) {
                tmpStr = "select LAST_INSERT_ID();";
            } else if (db.getDBType().compareTo("db2") == 0) {
                tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
            } else if (db.getDBType().compareTo("sqlite") == 0) {
                tmpStr = "select seq from sqlite_sequence where name = 'taudb_view'";
            } else if (db.getDBType().compareTo("derby") == 0) {
                tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
            } else if (db.getDBType().compareTo("h2") == 0) {
                tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
            } else if (db.getDBType().compareTo("oracle") == 0) {
                tmpStr = "SELECT " + db.getSchemaPrefix() + "taudb_view_id_seq.currval FROM DUAL";
            } else { // postgresql 
                tmpStr = "select currval('taudb_view_id_seq');";
            }
            newViewID = Integer.parseInt(db.getDataItem(tmpStr));
        }
        return newViewID;

    }
	
	public static int saveView(DB db, String name, String conjoin, int parent) throws SQLException {

		StringBuffer buf = new StringBuffer();
		buf.append("INSERT INTO " + db.getSchemaPrefix()
				+ "taudb_view  (parent, name, conjoin) ");

		buf.append(" VALUES (?,?,?)");
		PreparedStatement statement;
			statement = db.prepareStatement(buf.toString());
			if (parent >= 0)
				statement.setInt(1, parent);
			else
				statement.setNull(1, java.sql.Types.INTEGER);
			statement.setString(2, name);
			statement.setString(3, conjoin);
//			System.out.println(statement.toString());

			statement.executeUpdate();
			statement.close();

			String tmpStr = new String();
			if (db.getDBType().compareTo("mysql") == 0) {
				tmpStr = "select LAST_INSERT_ID();";
			} else if (db.getDBType().compareTo("db2") == 0) {
				tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
			} else if (db.getDBType().compareTo("sqlite") == 0) {
				tmpStr = "select seq from sqlite_sequence where name = 'taudb_view'";
			} else if (db.getDBType().compareTo("derby") == 0) {
				tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
			} else if (db.getDBType().compareTo("h2") == 0) {
				tmpStr = "select IDENTITY_VAL_LOCAL() FROM taudb_view";
			} else if (db.getDBType().compareTo("oracle") == 0) {
				tmpStr = "SELECT " + db.getSchemaPrefix()
						+ "taudb_view_id_seq.currval FROM DUAL";
			} else { // postgresql
				tmpStr = "select currval('taudb_view_id_seq');";
			}
			return Integer.parseInt(db.getDataItem(tmpStr));
	}
	public static void saveViewParameter(DB db, int viewID, String table, String column, String operator, String value) throws SQLException {

		StringBuffer buf = new StringBuffer();
		buf.append("INSERT INTO "
				+ db.getSchemaPrefix()
				+ "taudb_view_parameter   (taudb_view, table_name, column_name, operator, value) ");
		buf.append(" VALUES (?,?,?,?,?)");
		PreparedStatement statement;

			statement = db.prepareStatement(buf.toString());

			statement.setInt(1, viewID);
			statement.setString(2, table);
			statement.setString(3, column);
			statement.setString(4, operator);
			statement.setString(5, value);
//			System.out.println(statement.toString());

			statement.executeUpdate();
			statement.close();

	}

public static void deleteView(int viewID, DB db) throws SQLException{
	
		ArrayList<Integer> allChildern = getAllChildern(viewID, db);

		PreparedStatement statementView = null;
		PreparedStatement statementParam = null;

		statementView = db.prepareStatement("DELETE FROM "
				+ db.getSchemaPrefix()
				+ "taudb_view WHERE id = ?");

		statementParam = db.prepareStatement("DELETE FROM "
				+ db.getSchemaPrefix()
				+ "taudb_view_parameter WHERE taudb_view = ?");
		statementView.setInt(1, viewID);
		statementView.addBatch();
		statementParam.setInt(1, viewID);
		statementParam.addBatch();

		for (Integer i : allChildern) {
			statementView.setInt(1, i);
			statementView.addBatch();
			statementParam.setInt(1, i);
			statementParam.addBatch();
		}
		statementParam.executeBatch();
		statementView.executeBatch();
}
	public static ArrayList<Integer> getAllChildern(int viewID, DB db) throws SQLException {
	    ArrayList<Integer> childern = new ArrayList<Integer>();	    
	    getAllChildern(viewID,db,childern);
	    return childern;
}

	private static void getAllChildern(int id, DB db,
			ArrayList<Integer> childern) throws SQLException {
		PreparedStatement statement;
	    statement = db.prepareStatement(" SELECT id FROM " + db.getSchemaPrefix() + "taudb_view WHERE parent = ?");
	    statement.setInt(1, id);
	    ResultSet r = statement.executeQuery();	
	    while(r.next()){
	    	int child = r.getInt(1);
	    	childern.add(child);
		    getAllChildern(child,db,childern);
	    }
	}

	public int getNumFields() {
		return this.getFieldCount();
	}

	public String getWhereClause(String dbType) {
		if (trialID.length() > 0) {
			String tmpWhere = " where t.id in " + trialID;
			return tmpWhere;
		}
		if (whereClause == null || whereClause.equals("")) {
			StringBuilder wc = new StringBuilder();
			if (dbType.compareTo("db2") == 0) {
				wc.append(" cast (");
			}
			if (getField("TABLE_NAME").equalsIgnoreCase("Application")) {
				wc.append (" a.");
			} else if (getField("TABLE_NAME").equalsIgnoreCase("Experiment")) {
				wc.append (" e.");
			} else /*if (view.getField("table_name").equalsIgnoreCase("Trial")) */ {
				wc.append (" t.");
			}
			wc.append (getField("COLUMN_NAME"));
			if (dbType.compareTo("db2") == 0) {
				wc.append(" as varchar(256)) ");
			}

			wc.append (" " + getField("OPERATOR") + " '");
			wc.append (getField("VALUE"));
			wc.append ("' ");
			setWhereClause(wc.toString());
		}
		return whereClause;
	}

	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}


}
