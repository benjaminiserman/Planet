shader_type spatial;

//void vertex() {
	//// Called for every vertex the material is visible on.
	//VERTEX *= 1.0f + pow(COLOR.a, 5.0f) * 0.05f;
//}

void fragment() {
	// Called for every pixel the material is visible on.
	vec4 n_out2p0 = COLOR;
	ALBEDO = vec3(n_out2p0.xyz);
}

//void light() {
//	// Called for every pixel for every light affecting the material.
//	// Uncomment to replace the default light processing function with this one.
//}
