package ibis.group;

import java.lang.reflect.Method;

public final class GroupMethod { 

        public String name;
	public String description;

        public Class returnType;
        public Class [] parameters;
	public Class [] personalizeParameters;
	public Class [] combineParameters;

	/* vvvvvvvvvvvvvvvvvvvvv Only used in stub vvvvvvvvvvvvvvvvvvvv */

        public byte invocationMode;
        public byte resultMode;        

	/* For Group.REMOTE */
        public int destinationMember;
        public int destinationRank;
        public int destinationSkeleton;

	/* For Group.PERSONALIZE */
	public Class personalizeClass;
	public String personalizeMethodName;
	public Method personalizeMethod;

	/* For Group.COMBINE */
	public Class combineClass;
	public String combineMethodName;
	public Method combineMethod;       
}
