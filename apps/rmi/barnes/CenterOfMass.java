/* $Id$ */

import java.io.Serializable;

strictfp
final public class CenterOfMass implements Serializable {

    double cofmMass;
    double cofmCenter_x;
    double cofmCenter_y;
    double cofmCenter_z;
    double cofmCenterOfMass_x;
    double cofmCenterOfMass_y;
    double cofmCenterOfMass_z;

    CenterOfMass() {

	cofmMass = 0.0;

	cofmCenter_x = 0.0;
	cofmCenter_y = 0.0;
	cofmCenter_z = 0.0;

	cofmCenterOfMass_x = 0.0;
	cofmCenterOfMass_y = 0.0;
	cofmCenterOfMass_z = 0.0;
    }

    CenterOfMass( double mass, double cx, double cy, double cz, double cmx, double cmy, double cmz ) {

	cofmMass = mass;

	cofmCenter_x = cx;
	cofmCenter_y = cy;
	cofmCenter_z = cz;

	cofmCenterOfMass_x = cmx;
	cofmCenterOfMass_y = cmy;
	cofmCenterOfMass_z = cmz;
    }


}
