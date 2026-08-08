shader_type sky;

const vec3 BACKGROUND = vec3(0.0, 0.0, 0.001);
const vec3 RED = vec3(1.0, 0.55, 0.45);
const vec3 BLUE = vec3(0.6, 0.7, 1.0);
const vec3 YELLOW = vec3(1.0, 0.9, 0.6);
const vec3 WHITE = vec3(1.0, 1.0, 1.0);

uniform float star_density : hint_range(50.0, 500.0) = 400.0;
uniform float star_threshold : hint_range(0.9, 0.999) = 0.997;
uniform float star_brightness : hint_range(0.0, 3.0) = 1.5;
uniform float max_star_size : hint_range(0.05, 0.5) = 0.3;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + 0.1);
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

void sky() {
    vec3 dir = EYEDIR;
    vec3 color = BACKGROUND;

    vec3 cell = floor(dir * star_density);
    float h = hash(cell);

    if (h > star_threshold) {
        float brightness = hash(cell + 1.0);
        float colorPick = hash(cell + 2.0);

        vec3 star_color;
        if (colorPick < 0.55) {
            star_color = WHITE;
        } else if (colorPick < 0.75) {
            star_color = YELLOW;
        } else if (colorPick < 0.9) {
            star_color = RED;
        } else {
            star_color = BLUE;
        }

        color += star_color * brightness * star_brightness;
    }

    COLOR = color;
}