shader_type spatial;

void vertex() {
	// Called for every vertex the material is visible on.
	float r = 2.0f * asin(VERTEX.y) / PI;
	VERTEX *= 1.0f + (COLOR.a * 0.5f);
	//float r = 2.0f * asin(VERTEX.y) / PI;
	VERTEX *= (sin(r * 45.0f + TIME * 10.0f) * 0.1f) + 1.0f;
}

void fragment() {
	// Called for every pixel the material is visible on.
	vec4 n_out2p0 = COLOR;
	ALBEDO = vec3(n_out2p0.xyz);
}

//void light() {
//	// Called for every pixel for every light affecting the material.
//	// Uncomment to replace the default light processing function with this one.
//}
