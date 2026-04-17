# Load Test Fixtures Attribution

All images in this directory are sourced from Unsplash and used under the
Unsplash License (https://unsplash.com/license), which permits free use
for commercial and non-commercial purposes, modification, and
distribution without attribution requirement. Attribution is provided
here as good practice.

Photographer names, usernames and official descriptions were retrieved
via the Unsplash API at https://api.unsplash.com/photos/{id}.

## Image Mapping

Images are renamed to `fridge{1..10}.jpg` for stable test ordering. The
first five are rich multi-ingredient scenes used to exercise the L2
semantic cache path. The last five are single-ingredient close-ups used
to verify behavior when scans produce short ingredient lists.

### Multi-ingredient scenes

| File | Photographer | Photo ID | Link | Official description (Unsplash) | Content breakdown |
|---|---|---|---|---|---|
| fridge1.jpg | Anna Pelzer (@annapelzer) | IGfIGP5ONV0 | https://unsplash.com/photos/IGfIGP5ONV0 | Vegan salad bowl. Alt: "bowl of vegetable salads" | Buddha bowl with sliced avocado, cherry tomatoes on the vine, chickpeas, diced sweet potato, shredded red cabbage, yellow bell pepper, watermelon radish, green lettuce, microgreens, tahini dressing |
| fridge2.jpg | Joanie Simon (@joaniesimon) | 2r8BzVYZIeo | https://unsplash.com/photos/2r8BzVYZIeo | Alt: "An overhead shot of fruits, seeds and spices in bowls" | Mise-en-place flatlay with halved pear, cilantro, coconut flakes, dark grapes, cherry tomatoes, blueberries, pumpkin seeds, paprika powder, garlic cloves |
| fridge3.jpg | Natalia Gusakova (@nataliaraylenegusakova) | W4a8fOvE_Yw | https://unsplash.com/photos/W4a8fOvE_Yw | Alt: "a plate of food" | Raw pork shoulder on a plate with red bell pepper, fresh spinach, flat-leaf parsley, whole kumquats, ginger root, garlic bulb, small bowl of breadcrumbs |
| fridge4.jpg | ThermoPro (@thermopro) | wAkmA9I54dY | https://unsplash.com/photos/wAkmA9I54dY | All the ingredients required for a delicious pot roasted beef dinner. | Raw beef chuck roast, baby red potatoes, chopped onion, chopped celery, sliced carrots, minced garlic, fresh thyme and rosemary, soy sauce, meat thermometer |
| fridge5.jpg | Anh Nguyen (@pwign) | kcA-c3f_3FE | https://unsplash.com/photos/kcA-c3f_3FE | Alt: "vegetable and meat on bowl" | Salad bowl with marinated tofu cubes, corn kernels, two hard-boiled eggs, cherry tomato slices, shredded red cabbage, diced cucumber, edamame beans, chopped scallions, green leaf lettuce |

### Single-ingredient close-ups

| File | Photographer | Photo ID | Link | Official description (Unsplash) | Content breakdown |
|---|---|---|---|---|---|
| fridge6.jpg | David Foodphototasty (@phototastyfood) | U5lLwx17rWs | https://unsplash.com/photos/U5lLwx17rWs | Alt: "Two steaks on a plate with parsley" | One larger cut of beef and one marbled tenderloin on a dark slate, garnished with fresh cilantro |
| fridge7.jpg | Sardar Faizan (@nexio) | Qbj0u6CDNRI | https://unsplash.com/photos/Qbj0u6CDNRI | Alt: "four potatoes sitting on a towel on the ground" | Five whole unwashed yellow potatoes with earthy skins on a grey textured cloth |
| fridge8.jpg | David Foodphototasty (@phototastyfood) | JJcT6VJWDlg | https://unsplash.com/photos/JJcT6VJWDlg | Alt: "a couple of pieces of cheese sitting on top of a wooden cutting board" | Two wedges of yellow hard cheese on a dark wood board with scattered basil leaves |
| fridge9.jpg | Laura W (@lawid) | V7-R9D8_Qh8 | https://unsplash.com/photos/V7-R9D8_Qh8 | Alt: "A white bowl filled with lots of different types of tomatoes" | Heirloom red beefsteak tomatoes and yellow pear tomatoes, still wet from rinsing |
| fridge10.jpg | Wesual Click (@wesual) | rsWZ-P9FbQ4 | https://unsplash.com/photos/rsWZ-P9FbQ4 | "Here you can see the crispy, wonderful smelling Franziskaner-loaf and rye whole-grain tin loaf all baked by Franziskaner bakery in Bozen (Italy)" | Three round rustic breads (sunflower seed, oat-topped, flour-dusted sourdough) on a dark wooden surface with a wheat ear |

## License Text

From https://unsplash.com/license (accessed 2026-04-17):

> Unsplash photos are made to be used freely. Our license reflects that.
>
> All photos can be downloaded and used for free
> - Commercial and non-commercial purposes
> - No permission needed (though attribution is appreciated!)
>
> What is not permitted:
> - Photos cannot be sold without significant modification.
> - Compiling photos from Unsplash to replicate a similar or competing service.

## Modifications

All images have been resized to max 1600 pixels wide and re-encoded as
JPEG quality 85 to reduce upload size during load testing. Originals are
not redistributed.
