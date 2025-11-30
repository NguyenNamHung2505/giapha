import { Pipe, PipeTransform } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LanguageService } from '../../core/services/language.service';

@Pipe({
  name: 'localeDate',
  standalone: true,
  pure: false // Make it impure to react to language changes
})
export class LocaleDatePipe implements PipeTransform {
  private datePipe: DatePipe;

  constructor(private languageService: LanguageService) {
    const locale = this.getLocale();
    this.datePipe = new DatePipe(locale);
  }

  transform(value: Date | string | number | null | undefined, format: string = 'mediumDate'): string | null {
    if (!value) return null;

    const locale = this.getLocale();
    this.datePipe = new DatePipe(locale);

    return this.datePipe.transform(value, format);
  }

  private getLocale(): string {
    const lang = this.languageService.getCurrentLanguage();
    return lang === 'en' ? 'en-US' : 'vi';
  }
}
